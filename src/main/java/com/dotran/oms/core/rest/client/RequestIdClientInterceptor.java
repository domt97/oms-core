package com.dotran.oms.core.rest.client;

import com.dotran.oms.core.logging.LogSanitizer;
import com.dotran.oms.core.logging.MdcKeys;
import com.dotran.oms.core.rest.logging.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Sends the current {@code requestId} (MDC, set by {@link RequestIdFilter} or a consumer) as {@code X-Request-Id} on an
 * outbound HTTP call, so one flow can be followed across services (workspace open question 46). Only a valid id is sent
 * ({@link LogSanitizer#isValidRequestId}); an id the request already carries is kept. Only {@code requestId} travels:
 * tenant ids are taken by the callee from its own path, never from this header.
 * <p>
 * MDC is per thread: a call from an executor without a {@code TaskDecorator}, or from a scheduled job or consumer that
 * has no {@code requestId}, sends no header — open an {@code MdcScope} with one first.
 */
public class RequestIdClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String requestId = MDC.get(MdcKeys.REQUEST_ID);
        if (LogSanitizer.isValidRequestId(requestId) && !request.getHeaders().containsKey(RequestIdFilter.HEADER)) {
            request.getHeaders().set(RequestIdFilter.HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
