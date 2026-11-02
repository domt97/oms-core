package com.dotran.oms.core.rest.logging;

import com.dotran.oms.core.logging.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Puts the ids of the matched route into the MDC: every path variable named {@code ...Id} (merchantId, storeId,
 * productId, inventoryId, ...) whose value is a canonical UUID. A generic rule, so oms-core needs no knowledge of
 * each service's routes. Values arrive URL-decoded, hence the strict UUID check (no control characters can pass).
 * Runs after handler mapping, before the controller; exception handlers run before {@link #afterCompletion},
 * so their log lines carry the ids too.
 */
public class MdcPathVariableInterceptor implements HandlerInterceptor {

    private static final Pattern ID_NAME = Pattern.compile("^[a-z][A-Za-z]{0,30}Id$");
    private static final String ADDED = MdcPathVariableInterceptor.class.getName() + ".keys";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables) || variables.isEmpty()) {
            return true;
        }
        List<String> added = new ArrayList<>();
        variables.forEach((name, value) -> {
            if (name instanceof String key && value instanceof String id
                    && ID_NAME.matcher(key).matches() && LogSanitizer.isUuid(id)) {
                MDC.put(key, id);
                added.add(key);
            }
        });
        request.setAttribute(ADDED, added);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getAttribute(ADDED) instanceof List<?> keys) {
            keys.forEach(key -> MDC.remove((String) key));
        }
    }
}
