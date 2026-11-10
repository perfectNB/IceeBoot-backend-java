package com.iceeboot.framework.service.system.impl;

import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.service.pay.client.YunGouWxApiClient;
import com.iceeboot.config.cache.RedisConfig;
import com.iceeboot.framework.service.system.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 只连接由本测试启动的独立 Redis，覆盖真实 Jackson 序列化和 Lua 执行。 */
@EnabledIfSystemProperty(named="iceeboot.test.redis-server", matches=".+")
class AuthRedisAtomicityTest {
    @TempDir static Path temp;
    static Process process;
    static LettuceConnectionFactory factory;
    static RedisTemplate<String, Object> redis;
    AuthServiceImpl service;
    ISysUserService users;

    @BeforeAll static void startRedis() throws Exception {
        int port;
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
        process = new ProcessBuilder(System.getProperty("iceeboot.test.redis-server"), "--bind", "127.0.0.1",
                "--port", String.valueOf(port), "--save", "", "--appendonly", "no", "--dir", temp.toString())
                .redirectErrorStream(true).redirectOutput(temp.resolve("redis.log").toFile()).start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        boolean ready = false;
        while (System.nanoTime() < deadline && process.isAlive()) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
                socket.setSoTimeout(500);
                socket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII));
                ready = "+PONG".equals(new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine());
                if (ready) break;
            } catch (IOException ignored) { Thread.sleep(25); }
        }
        assertThat(ready).as("独立测试 Redis 正常启动").isTrue();
        factory = new LettuceConnectionFactory("127.0.0.1", port);
        factory.afterPropertiesSet(); factory.start();
        redis = new RedisConfig().redisTemplate(factory);
    }

    @AfterAll static void stopRedis() throws Exception {
        if (factory != null) factory.destroy();
        if (process != null) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
        }
    }

    @BeforeEach void setup() {
        try (var connection = factory.getConnection()) { connection.serverCommands().flushDb(); }
        users = mock(ISysUserService.class);
        service = new AuthServiceImpl(users, mock(ISysUserRoleService.class),
                mock(ISocialService.class), redis, mock(TransactionTemplate.class), mock(YunGouWxApiClient.class));
        ReflectionTestUtils.setField(service, "refreshTokenExpireSeconds", 120L);
    }

    void store(String token) { ReflectionTestUtils.invokeMethod(service, "storeRefreshToken", 7L, token); }
    String pointer() { return (String) redis.opsForValue().get(CacheConstants.Auth.USER_REFRESH_TOKEN+7L); }

    @Test void replacementUsesExistingJsonFormatAndRemovesOldRefreshAndConsumedKeys() {
        store("old-fixture");
        redis.opsForValue().set(CacheConstants.Auth.REFRESH_TOKEN_CONSUMED+"old-fixture", "1");
        store("new-fixture");
        assertThat(pointer()).isEqualTo("new-fixture");
        assertThat(redis.hasKey(CacheConstants.Auth.REFRESH_TOKEN+"old-fixture")).isFalse();
        assertThat(redis.hasKey(CacheConstants.Auth.REFRESH_TOKEN_CONSUMED+"old-fixture")).isFalse();
        Object uid = redis.opsForValue().get(CacheConstants.Auth.REFRESH_TOKEN+"new-fixture");
        assertThat(uid.toString()).isEqualTo("7");
        assertThat(redis.getExpire(CacheConstants.Auth.REFRESH_TOKEN+"new-fixture")).isBetween(1L, 120L);
        String consumed = ReflectionTestUtils.invokeMethod(service, "consumeRefreshToken", "new-fixture");
        assertThat(consumed).isEqualTo("7");
    }

    @Test void revokeRemovesCurrentPointerAndBothTokenRecords() {
        store("fixture");
        redis.opsForValue().set(CacheConstants.Auth.REFRESH_TOKEN_CONSUMED+"fixture", "1");
        ReflectionTestUtils.invokeMethod(service, "revokeUserRefreshToken", 7L);
        assertThat(redis.keys("auth:*refresh*")).isEmpty();
    }

    @Test void concurrentReplacementLeavesExactlyOneUsableRefresh() throws Exception {
        int callers = 8;
        var start = new CyclicBarrier(callers);
        try (var executor = Executors.newFixedThreadPool(callers)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i=0; i<callers; i++) {
                String token = "fixture-"+i;
                tasks.add(executor.submit(() -> { start.await(); store(token); return null; }));
            }
            for (var task : tasks) task.get(10, TimeUnit.SECONDS);
        }
        assertThat(redis.keys(CacheConstants.Auth.REFRESH_TOKEN+"*")).containsExactly(CacheConstants.Auth.REFRESH_TOKEN+pointer());
    }

    @Test void concurrentRevokeAndReplaceNeverLeaveAnOrphanToken() throws Exception {
        for (int attempt=0; attempt<10; attempt++) {
            store("initial-"+attempt);
            var start = new CyclicBarrier(2);
            String next = "next-"+attempt;
            try (var executor = Executors.newFixedThreadPool(2)) {
                var write = executor.submit(() -> { start.await(); store(next); return null; });
                var revoke = executor.submit(() -> { start.await(); ReflectionTestUtils.invokeMethod(service, "revokeUserRefreshToken", 7L); return null; });
                write.get(10, TimeUnit.SECONDS); revoke.get(10, TimeUnit.SECONDS);
            }
            String current = pointer();
            var keys = redis.keys(CacheConstants.Auth.REFRESH_TOKEN+"*");
            if (current == null) assertThat(keys).isEmpty();
            else assertThat(keys).containsExactly(CacheConstants.Auth.REFRESH_TOKEN+current);
        }
    }

    @Test void concurrentStateVerificationSucceedsOnce() throws Exception {
        redis.opsForValue().set(CacheConstants.Auth.WECHAT_STATE+"fixture", "fixture", Duration.ofMinutes(1));
        var start = new CyclicBarrier(2);
        Callable<Boolean> verify = () -> { start.await(); return ReflectionTestUtils.invokeMethod(service, "verifyState", "fixture"); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(verify); var second = executor.submit(verify);
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
    }
    @Test void ticketIsConsumedOnceUsingTheExistingJsonCacheFormat() {
        var login = new com.alibaba.fastjson.JSONObject();
        login.put("userId", 7L); login.put("accessToken", "access-fixture");
        login.put("refreshToken", "refresh-fixture"); login.put("tokenTimeout", 7200L);
        redis.opsForValue().set(CacheConstants.Auth.WECHAT_TICKET+"fixture", login.toJSONString());
        when(users.getUserById(7L)).thenReturn(new com.iceeboot.framework.vo.system.SysUserVO());
        try (var tokens = mockStatic(cn.dev33.satoken.stp.StpUtil.class)) {
            tokens.when(() -> cn.dev33.satoken.stp.StpUtil.getLoginIdByToken("access-fixture")).thenReturn(7L);
            assertThat(service.exchangeTicket("fixture").getAccessToken()).isEqualTo("access-fixture");
            assertThatThrownBy(() -> service.exchangeTicket("fixture"))
                    .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        }
    }
}
