package com.dotran.oms.core.rest.logging;

import com.dotran.oms.core.logging.LogSanitizer;
import com.dotran.oms.core.logging.MdcKeys;
import com.dotran.oms.core.logging.MdcScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gives every HTTP request a {@code requestId} in the MDC and echoes it in the {@code X-Request-Id} response header.
 * A valid inbound header (as sent by oms-web) is kept, so the id shown in the browser matches the logs; anything else
 * is replaced by a new UUID and never echoed. Runs early (after Boot's character-encoding filter), before the request
 * logging filter and a future security chain, and also on the error dispatch.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    /** Request attribute: the id survives into Tomcat's /error dispatch. */
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolve(request);
        request.setAttribute(ATTRIBUTE, requestId);
        // Set before the chain: once the body is written the response is committed and headers are ignored.
        response.setHeader(HEADER, requestId);

        try (MdcScope mdc = MdcScope.open()) {
            mdc.put(MdcKeys.REQUEST_ID, requestId);
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /** An error dispatch nested inside this filter (e.g. sendError) keeps the same id. */
    @Override
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        doFilterInternal(request, response, filterChain);
    }

    static String resolve(HttpServletRequest request) {
        Object fromAttribute = request.getAttribute(ATTRIBUTE);
        if (fromAttribute instanceof String id) {
            return id;
        }
        String inbound = request.getHeader(HEADER);
        return LogSanitizer.isValidRequestId(inbound) ? inbound : UUID.randomUUID().toString();
    }
}
