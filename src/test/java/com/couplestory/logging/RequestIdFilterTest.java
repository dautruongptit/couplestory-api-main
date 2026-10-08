package com.couplestory.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private MockHttpServletResponse run(MockHttpServletRequest request, String[] idSeenByTheApp) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> idSeenByTheApp[0] = MDC.get("requestId");
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void echoesAWellFormedIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc12345-def");
        String[] seen = new String[1];

        MockHttpServletResponse response = run(request, seen);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc12345-def");
        assertThat(seen[0]).isEqualTo("abc12345-def");
        assertThat(request.getAttribute("requestId")).isEqualTo("abc12345-def");
    }

    @Test
    void generatesAnIdWhenNoneIsSent() throws Exception {
        String[] seen = new String[1];

        MockHttpServletResponse response = run(new MockHttpServletRequest(), seen);

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
        assertThat(seen[0]).isEqualTo(response.getHeader("X-Request-Id"));
    }

    @Test
    void replacesAnIdThatCouldInjectIntoLogsOrHeaders() throws Exception {
        for (String hostile : new String[]{"bad id with spaces", "x".repeat(200), "abc\r\nSet-Cookie: a=b", "<script>", "short"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", hostile);

            MockHttpServletResponse response = run(request, new String[1]);

            assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
        }
    }

    @Test
    void clearsTheLoggingContextAfterTheRequest() throws Exception {
        run(new MockHttpServletRequest(), new String[1]);

        assertThat(MDC.get("requestId")).isNull();
    }
}
