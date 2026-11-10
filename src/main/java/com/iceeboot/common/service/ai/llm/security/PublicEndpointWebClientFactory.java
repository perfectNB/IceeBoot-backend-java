package com.iceeboot.common.service.ai.llm.security;

import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 为用户自定义的公网 LLM 地址创建 WebClient。
 *
 * <p>解析结果会在同一次 Netty 连接中完成校验并直接用于建连，避免“先校验 DNS、
 * 请求时再次解析”带来的 DNS 重绑定窗口。平台自有 LLM 请求不使用这个客户端。</p>
 */
public final class PublicEndpointWebClientFactory {
    private static final AddressResolverGroup<InetSocketAddress> PUBLIC_RESOLVER =
            new PublicAddressResolverGroup();

    private PublicEndpointWebClientFactory() {
    }

    public static WebClient create(int maxInMemoryBytes) {
        HttpClient httpClient = HttpClient.create()
                .resolver(PUBLIC_RESOLVER)
                .followRedirect(false);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemoryBytes))
                .build();
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            if (first == 0 || first == 10 || first == 127 || first >= 224) return false;
            if (first == 100 && second >= 64 && second <= 127) return false;
            if (first == 169 && second == 254) return false;
            if (first == 172 && second >= 16 && second <= 31) return false;
            if (first == 192 && (second == 168 || second == 0 && (third == 0 || third == 2)
                    || second == 88 && third == 99)) return false;
            if (first == 198 && (second == 18 || second == 19 || second == 51 && third == 100)) return false;
            return !(first == 203 && second == 0 && third == 113);
        }
        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            if ((first & 0xfe) == 0xfc) return false; // fc00::/7 unique local
            if (first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0d && Byte.toUnsignedInt(bytes[3]) == 0xb8) {
                return false; // 2001:db8::/32 documentation range
            }
            if (isIpv4Mapped(bytes)) {
                try {
                    byte[] ipv4 = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
                    return isPublicAddress(InetAddress.getByAddress(ipv4));
                } catch (UnknownHostException impossible) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) return false;
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static InetSocketAddress requirePublic(InetSocketAddress resolved) throws UnknownHostException {
        if (resolved == null || resolved.getAddress() == null || !isPublicAddress(resolved.getAddress())) {
            throw new UnknownHostException("API URL 解析到了本机、局域网或保留地址");
        }
        return resolved;
    }

    private static final class PublicAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {
        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            AddressResolver<InetSocketAddress> delegate = DefaultAddressResolverGroup.INSTANCE.getResolver(executor);
            return new PublicAddressResolver(delegate, executor);
        }
    }

    private static final class PublicAddressResolver implements AddressResolver<InetSocketAddress> {
        private final AddressResolver<InetSocketAddress> delegate;
        private final EventExecutor executor;

        private PublicAddressResolver(AddressResolver<InetSocketAddress> delegate, EventExecutor executor) {
            this.delegate = delegate;
            this.executor = executor;
        }

        @Override
        public boolean isSupported(SocketAddress address) {
            return delegate.isSupported(address);
        }

        @Override
        public boolean isResolved(SocketAddress address) {
            if (!delegate.isResolved(address)) return false;
            return address instanceof InetSocketAddress inetSocketAddress
                    && inetSocketAddress.getAddress() != null
                    && isPublicAddress(inetSocketAddress.getAddress());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address) {
            return resolve(address, executor.newPromise());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address, Promise<InetSocketAddress> promise) {
            delegate.resolve(address).addListener(future -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                try {
                    promise.trySuccess(requirePublic((InetSocketAddress) future.getNow()));
                } catch (Exception error) {
                    promise.tryFailure(error);
                }
            });
            return promise;
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address) {
            return resolveAll(address, executor.newPromise());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address,
                                                           Promise<List<InetSocketAddress>> promise) {
            delegate.resolveAll(address).addListener(future -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                try {
                    List<InetSocketAddress> resolved = (List<InetSocketAddress>) future.getNow();
                    for (InetSocketAddress candidate : resolved) requirePublic(candidate);
                    promise.trySuccess(resolved);
                } catch (Exception error) {
                    promise.tryFailure(error);
                }
            });
            return promise;
        }

        @Override
        public void close() {
            // delegate 属于 Netty 的共享 DefaultAddressResolverGroup，由其生命周期统一管理。
        }
    }
}
