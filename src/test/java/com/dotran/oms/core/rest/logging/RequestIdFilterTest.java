package com.dotran.oms.core.rest.logging;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private static String run(RequestIdFilter filter, MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(MDC.get("requestId"));
        filter.doFilter(request, response, chain);
        return seen.get();
    }

    @Test
    @DisplayName("a valid X-Request-Id is put in MDC, echoed, and MDC is empty afterwards")
    void keepsValidId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/store/x");
        request.addHeader(RequestIdFilter.HEADER, "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String inChain = run(filter, request, response);

        assertThat(inChain).isEqualTo("3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(inChain);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a b", "a\nINFO fake", "<script>", "\u00e9"})
    @DisplayName("an invalid header is replaced by a new UUID and never echoed")
    void replacesInvalidId(String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(RequestIdFilter.HEADER, header);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String inChain = run(filter, request, response);

        assertThat(inChain).isNotEqualTo(header);
        assertThat(UUID.fromString(inChain)).isNotNull();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(inChain);
    }

    @Test
    @DisplayName("missing and too long (65) headers get a new UUID")
    void missingOrTooLong() throws Exception {
        MockHttpServletRequest none = new MockHttpServletRequest("GET", "/x");
        assertThat(UUID.fromString(run(filter, none, new MockHttpServletResponse()))).isNotNull();

        MockHttpServletRequest tooLong = new MockHttpServletRequest("GET", "/x");
        tooLong.addHeader(RequestIdFilter.HEADER, "a".repeat(65));
        assertThat(run(filter, tooLong, new MockHttpServletResponse())).hasSize(36);
    }

    @Test
    @DisplayName("MDC is restored when the chain throws, keeping keys that existed before")
    void restoresOnException() {
        MDC.put("outer", "kept");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), failing))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("outer")).isEqualTo("kept");
    }

    @Test
    @DisplayName("the error dispatch reuses the id of the original request")
    void errorDispatchReusesId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        String first = run(filter, request, new MockHttpServletResponse());

        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute(org.springframework.web.util.WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/x");
        String onError = run(filter, request, new MockHttpServletResponse());

        assertThat(onError).isEqualTo(first);
    }

    @Test
    @DisplayName("with several X-Request-Id headers the first one is used")
    void firstHeaderWins() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(RequestIdFilter.HEADER, "first-id");
        request.addHeader(RequestIdFilter.HEADER, "second-id");

        assertThat(run(filter, request, new MockHttpServletResponse())).isEqualTo("first-id");
    }
}
