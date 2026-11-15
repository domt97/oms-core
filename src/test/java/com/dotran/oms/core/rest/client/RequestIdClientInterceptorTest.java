package com.dotran.oms.core.rest.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestIdClientInterceptor")
class RequestIdClientInterceptorTest {

    private final RequestIdClientInterceptor interceptor = new RequestIdClientInterceptor();
    private final AtomicInteger calls = new AtomicInteger();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private MockClientHttpRequest call(MockClientHttpRequest request) throws Exception {
        byte[] body = "{}".getBytes();
        interceptor.intercept(request, body, (r, b) -> {
            calls.incrementAndGet();
            assertThat(b).isSameAs(body);
            return new MockClientHttpResponse(new byte[0], 200);
        });
        return request;
    }

    private static MockClientHttpRequest request() {
        return new MockClientHttpRequest(HttpMethod.POST, URI.create("http://stores/internal/x"));
    }

    @Test
    @DisplayName("a valid requestId in the MDC is sent as X-Request-Id; the call runs once with the same body")
    void validId_sent() throws Exception {
        MDC.put("requestId", "abc-123_X.y");

        MockClientHttpRequest sent = call(request());

        assertThat(sent.getHeaders().get("X-Request-Id")).containsExactly("abc-123_X.y");
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("no requestId in the MDC → no header")
    void noId_noHeader() throws Exception {
        assertThat(call(request()).getHeaders().containsKey("X-Request-Id")).isFalse();
        assertThat(calls).hasValue(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "has space", "line\nbreak", "semi;colon"})
    @DisplayName("an invalid requestId (blank, space, control character, symbol) is never sent")
    void invalidId_notSent(String id) throws Exception {
        MDC.put("requestId", id);

        assertThat(call(request()).getHeaders().containsKey("X-Request-Id")).isFalse();
    }

    @Test
    @DisplayName("a requestId longer than 64 characters is never sent")
    void tooLong_notSent() throws Exception {
        MDC.put("requestId", "a".repeat(65));

        assertThat(call(request()).getHeaders().containsKey("X-Request-Id")).isFalse();
    }

    @Test
    @DisplayName("an X-Request-Id the request already has is kept, not overwritten or doubled")
    void existingHeader_kept() throws Exception {
        MDC.put("requestId", "from-mdc");
        MockClientHttpRequest request = request();
        request.getHeaders().set("X-Request-Id", "explicit");

        assertThat(call(request).getHeaders().get("X-Request-Id")).containsExactly("explicit");
    }
}
