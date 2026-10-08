package com.couplestory.logging;

import com.couplestory.security.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiLogFilterTest {

    private final List<ApiLog> saved = new ArrayList<>();
    private ApiLogWriter writer;
    private ApiLogFilter filter;

    @BeforeEach
    void setUp() {
        writer = new ApiLogWriter(saved::addAll, 10);
        filter = new ApiLogFilter(writer, new ClientIpResolver());
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.7");
        return request;
    }

    private void run(MockHttpServletRequest request, FilterChain chain) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    private List<ApiLog> flushed() {
        writer.flush();
        return saved;
    }

    @Test
    void recordsTheRouteTemplateAndNeverTheQueryString() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/stories/123");
        request.setQueryString("token=secret-abc");
        request.addHeader("User-Agent", "JUnit/1.0");

        run(request, (req, res) -> {
            req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/stories/{id}");
            ((MockHttpServletResponse) res).setStatus(200);
        });

        ApiLog log = flushed().get(0);
        assertThat(log.getRoute()).isEqualTo("/api/stories/{id}");
        assertThat(log.getMethod()).isEqualTo("GET");
        assertThat(log.getStatus()).isEqualTo((short) 200);
        assertThat(log.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(log.getIp()).isEqualTo("203.0.113.7");
        assertThat(log.getUserAgent()).isEqualTo("JUnit/1.0");
        assertThat(log.toString()).doesNotContain("secret-abc");
    }

    @Test
    void replacesIdsInTheUriWhenNoHandlerMatched() throws Exception {
        String uri = "/api/stories/" + UUID.randomUUID() + "/photos/12345";

        run(request("GET", uri), (req, res) -> ((MockHttpServletResponse) res).setStatus(401));

        assertThat(flushed().get(0).getRoute()).isEqualTo("/api/stories/{id}/photos/{id}");
    }

    @Test
    void stillRecordsRequestsRejectedBeforeAnyHandlerRuns() throws Exception {
        run(request("POST", "/api/stories"), (req, res) -> ((MockHttpServletResponse) res).setStatus(401));

        assertThat(flushed()).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo((short) 401);
    }

    @Test
    void skipsHealthChecksUploadsAndPreflights() throws Exception {
        FilterChain ok = (req, res) -> { };
        run(request("GET", "/actuator/health"), ok);
        run(request("GET", "/uploads/photo.webp"), ok);
        run(request("OPTIONS", "/api/stories"), ok);

        assertThat(flushed()).isEmpty();
    }

    @Test
    void recordsTheAuthenticatedUserAndTheRequestId() throws Exception {
        UUID user = UUID.randomUUID();
        MockHttpServletRequest request = request("GET", "/api/stories");
        request.setAttribute("requestId", "req-1234567");

        run(request, (req, res) -> req.setAttribute("auth.userId", user));

        ApiLog log = flushed().get(0);
        assertThat(log.getUserId()).isEqualTo(user);
        assertThat(log.getRequestId()).isEqualTo("req-1234567");
    }

    @Test
    void recordsA500WhenTheChainThrowsAndRethrows() {
        assertThatThrownBy(() -> run(request("GET", "/api/stories"), (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(flushed().get(0).getStatus()).isEqualTo((short) 500);
    }

    @Test
    void neverBlocksOrFailsTheRequestWhenTheQueueIsFull() throws Exception {
        ApiLogWriter tiny = new ApiLogWriter(batch -> { }, 1);
        ApiLogFilter tinyFilter = new ApiLogFilter(tiny, new ClientIpResolver());

        tinyFilter.doFilter(request("GET", "/api/a"), new MockHttpServletResponse(), (req, res) -> { });
        tinyFilter.doFilter(request("GET", "/api/b"), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(tiny.dropped()).isEqualTo(1);
    }

    @Test
    void cutsOverlongValuesToTheColumnSizes() throws Exception {
        MockHttpServletRequest request = request("GET", "/" + "a".repeat(400));
        request.addHeader("User-Agent", "u".repeat(500));

        run(request, (req, res) -> { });

        ApiLog log = flushed().get(0);
        assertThat(log.getRoute()).hasSizeLessThanOrEqualTo(200);
        assertThat(log.getUserAgent()).hasSizeLessThanOrEqualTo(255);
    }
}
