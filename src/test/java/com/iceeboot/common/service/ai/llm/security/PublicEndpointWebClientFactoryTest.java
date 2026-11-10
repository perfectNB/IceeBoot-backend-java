package com.iceeboot.common.service.ai.llm.security;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEndpointWebClientFactoryTest {

    @Test
    void shouldRejectPrivateReservedAndMappedAddresses() throws Exception {
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("127.0.0.1"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("10.10.1.2"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("100.64.0.1"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("169.254.169.254"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("198.18.0.1"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("fc00::1"))).isFalse();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("::ffff:127.0.0.1"))).isFalse();
    }

    @Test
    void shouldAllowPublicIpv4AndIpv6Addresses() throws Exception {
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(InetAddress.getByName("8.8.8.8"))).isTrue();
        assertThat(PublicEndpointWebClientFactory.isPublicAddress(
                InetAddress.getByName("2606:4700:4700::1111"))).isTrue();
    }
}
