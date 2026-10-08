package com.couplestory.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    private MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void cloudflareHeaderWinsOverForwardedFor() {
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "198.51.100.9, 172.18.0.2");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void usesTheFirstForwardedForEntryWhenThereIsNoCloudflareHeader() {
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("X-Forwarded-For", " 198.51.100.9 , 172.18.0.2");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void fallsBackToTheConnectionAddress() {
        assertThat(resolver.resolve(request("172.18.0.5"))).isEqualTo("172.18.0.5");
    }

    @Test
    void ignoresGarbageInTheHeadersAndFallsBack() {
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "<script>alert(1)</script>");
        request.addHeader("X-Forwarded-For", "999.1.1.1, not-an-ip");

        assertThat(resolver.resolve(request)).isEqualTo("172.18.0.5");
    }

    @Test
    void keepsAValidIpv6Address() {
        MockHttpServletRequest request = request("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "2001:db8::1");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void neverReturnsMoreThan45Characters() {
        MockHttpServletRequest request = request("x".repeat(100));

        assertThat(resolver.resolve(request)).hasSizeLessThanOrEqualTo(45);
    }
}
