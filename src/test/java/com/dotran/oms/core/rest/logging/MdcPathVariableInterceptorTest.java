package com.dotran.oms.core.rest.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MdcPathVariableInterceptorTest {

    private static final String M = "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab";
    private static final String S = "9b880f8b-6fa1-4ea6-af81-28586e2edbd7";

    private final MdcPathVariableInterceptor interceptor = new MdcPathVariableInterceptor();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private MockHttpServletRequest requestWith(Map<String, String> variables) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables);
        return request;
    }

    @Test
    @DisplayName("...Id variables with UUID values go into MDC and are removed after completion")
    void putsAndRemovesIds() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("merchantId", M);
        vars.put("storeId", S);
        vars.put("storeAvailabilityId", S);
        MockHttpServletRequest request = requestWith(vars);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(MDC.get("merchantId")).isEqualTo(M);
        assertThat(MDC.get("storeId")).isEqualTo(S);
        assertThat(MDC.get("storeAvailabilityId")).isEqualTo(S);

        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(MDC.get("merchantId")).isNull();
        assertThat(MDC.get("storeId")).isNull();
        assertThat(MDC.get("storeAvailabilityId")).isNull();
    }

    @Test
    @DisplayName("non-UUID values (incl. decoded newlines) and names not ending in Id are ignored")
    void ignoresInvalid() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("storeId", "abc");
        vars.put("productId", "1-2-3-4-5");
        vars.put("inventoryId", M + "\nINFO fake");
        vars.put("id", M);
        vars.put("code", M);
        MockHttpServletRequest request = requestWith(vars);

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("keys that were in MDC before (e.g. requestId) are kept after completion")
    void keepsOtherKeys() {
        MDC.put("requestId", "r1");
        MockHttpServletRequest request = requestWith(Map.of("storeId", S));

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("requestId")).isEqualTo("r1");
        assertThat(MDC.get("storeId")).isNull();
    }

    @Test
    @DisplayName("no template variables (e.g. 404 without a handler) is fine")
    void noVariables() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
