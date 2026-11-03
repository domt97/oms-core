package com.dotran.oms.core.rest.logging;

import com.dotran.oms.core.logging.MdcProperties;
import com.dotran.oms.core.logging.MdcProperties.Field;
import com.dotran.oms.core.logging.MdcProperties.Source;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Puts the request values configured in {@code app.logging.mdc.fields} ({@link MdcProperties}) into the MDC:
 * path variables of the matched route and, where configured, query string parameters. A value is added only if it
 * matches its field's format and has no control characters ({@link MdcProperties.Field#accepts}); values arrive
 * URL-decoded, so this check is what keeps newlines and junk out of the logs. Writes the MDC directly (not through
 * {@code MdcScope}) and removes its own keys; {@code RequestIdFilter}'s scope restores the rest. Runs after
 * handler mapping, before the controller; exception handlers run before {@link #afterCompletion}, so their lines
 * carry the keys too.
 */
public class MdcRequestFieldsInterceptor implements HandlerInterceptor {

    private static final String ADDED = MdcRequestFieldsInterceptor.class.getName() + ".keys";

    private final MdcProperties properties;

    public MdcRequestFieldsInterceptor(MdcProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        List<String> added = new ArrayList<>();
        Map<?, ?> variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> map
                ? map : Map.of();

        properties.fieldsFrom(Source.PATH).forEach((key, field) ->
                put(key, field, variables.get(key) instanceof String value ? value : null, added));

        Map<String, Field> queryFields = properties.fieldsFrom(Source.QUERY);
        if (!queryFields.isEmpty() && request.getQueryString() != null) {
            Map<String, String> query = firstQueryValues(request.getQueryString());
            queryFields.forEach((key, field) -> {
                // The route's own path variable wins, even when its value was rejected.
                if (!variables.containsKey(key)) {
                    put(key, field, query.get(key), added);
                }
            });
        }

        if (!added.isEmpty()) {
            request.setAttribute(ADDED, added);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getAttribute(ADDED) instanceof List<?> keys) {
            keys.forEach(key -> MDC.remove((String) key));
        }
    }

    /** First value per name from the raw query string only (not form bodies, unlike getParameter). */
    static Map<String, String> firstQueryValues(String queryString) {
        Map<String, String> values = new java.util.HashMap<>();
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf('=');
            String name = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            if (name != null && value != null) {
                values.putIfAbsent(name, value);
            }
        }
        return values;
    }

    private static String decode(String raw) {
        try {
            return java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null; // malformed escape: skip
        }
    }

    private static void put(String key, Field field, String value, List<String> added) {
        if (field.accepts(value)) {
            MDC.put(key, value);
            added.add(key);
        }
    }
}
