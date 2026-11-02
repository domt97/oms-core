package com.dotran.oms.core.rest.logging;

import com.dotran.oms.core.logging.MdcProperties;
import com.dotran.oms.core.logging.MdcProperties.Field;
import com.dotran.oms.core.logging.MdcProperties.Format;
import com.dotran.oms.core.logging.MdcProperties.Source;
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

class MdcRequestFieldsInterceptorTest {

    private static final String M = "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab";
    private static final String S = "9b880f8b-6fa1-4ea6-af81-28586e2edbd7";

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private static MdcRequestFieldsInterceptor interceptor(MdcProperties properties) {
        properties.validate();
        return new MdcRequestFieldsInterceptor(properties);
    }

    private static MdcProperties withExtra(String key, Field field) {
        MdcProperties properties = new MdcProperties();
        properties.getFields().put(key, field);
        return properties;
    }

    private static MockHttpServletRequest requestWith(Map<String, String> variables) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables);
        return request;
    }

    @Test
    @DisplayName("default fields: UUID path ids go into MDC and are removed after completion")
    void defaultsPutAndRemove() {
        MdcRequestFieldsInterceptor interceptor = interceptor(new MdcProperties());
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
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("values not matching the format (incl. decoded newlines) and unconfigured names are ignored")
    void ignoresInvalidAndUnconfigured() {
        MdcRequestFieldsInterceptor interceptor = interceptor(new MdcProperties());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("storeId", "abc");
        vars.put("productId", "1-2-3-4-5");
        vars.put("inventoryId", M + "\nINFO fake");
        vars.put("id", M);
        vars.put("orderId", M);
        MockHttpServletRequest request = requestWith(vars);

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("a field configured by the service (code format) is read from path and query")
    void configuredCodeFromPathAndQuery() {
        MdcRequestFieldsInterceptor interceptor =
                interceptor(withExtra("sku", Field.of(Format.CODE, false, Source.PATH, Source.QUERY)));

        MockHttpServletRequest fromPath = requestWith(Map.of("sku", "TS-M.01"));
        interceptor.preHandle(fromPath, response, new Object());
        assertThat(MDC.get("sku")).isEqualTo("TS-M.01");
        interceptor.afterCompletion(fromPath, response, new Object(), null);
        assertThat(MDC.get("sku")).isNull();

        MockHttpServletRequest fromQuery = new MockHttpServletRequest("GET", "/x");
        fromQuery.setQueryString("sku=TS-L&sku=OTHER");
        interceptor.preHandle(fromQuery, response, new Object());
        assertThat(MDC.get("sku")).isEqualTo("TS-L");

        MDC.clear();
        MockHttpServletRequest bad = new MockHttpServletRequest("GET", "/x");
        bad.setQueryString("sku=a%0AINFO%20fake");
        interceptor.preHandle(bad, response, new Object());
        assertThat(MDC.get("sku")).isNull();
    }

    @Test
    @DisplayName("default fields are not read from the query; the path value wins over the query")
    void pathOnlyByDefaultAndPathWins() {
        MdcRequestFieldsInterceptor interceptor =
                interceptor(withExtra("sku", Field.of(Format.CODE, false, Source.PATH, Source.QUERY)));
        MockHttpServletRequest request = requestWith(Map.of("sku", "FROM-PATH"));
        request.setQueryString("sku=FROM-QUERY&storeId=" + S);

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("sku")).isEqualTo("FROM-PATH");
        assertThat(MDC.get("storeId")).isNull();
    }

    @Test
    @DisplayName("a pattern field accepts only matching values up to the length cap")
    void patternField() {
        Field orderNo = Field.of(Format.PATTERN, false, Source.PATH);
        orderNo.setPattern("^ORD-[0-9]{1,12}$");
        MdcRequestFieldsInterceptor interceptor = interceptor(withExtra("orderNo", orderNo));

        interceptor.preHandle(requestWith(Map.of("orderNo", "ORD-42")), response, new Object());
        assertThat(MDC.get("orderNo")).isEqualTo("ORD-42");

        MDC.clear();
        interceptor.preHandle(requestWith(Map.of("orderNo", "X-42")), response, new Object());
        assertThat(MDC.get("orderNo")).isNull();
    }

    @Test
    @DisplayName("requestId is never taken from path or query; other MDC keys are kept after completion")
    void requestIdNotFromRequestAndOtherKeysKept() {
        MdcRequestFieldsInterceptor interceptor = interceptor(new MdcProperties());
        MDC.put("requestId", "r1");
        MockHttpServletRequest request = requestWith(Map.of("requestId", "evil", "storeId", S));
        request.setQueryString("requestId=evil");

        interceptor.preHandle(request, response, new Object());
        assertThat(MDC.get("requestId")).isEqualTo("r1");
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("requestId")).isEqualTo("r1");
        assertThat(MDC.get("storeId")).isNull();
    }

    @Test
    @DisplayName("no template variables (e.g. 404 without a handler) is fine")
    void noVariables() {
        MdcRequestFieldsInterceptor interceptor = interceptor(new MdcProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("a permissive pattern still never lets control characters into the MDC")
    void permissivePatternRejectsControlChars() {
        Field note = Field.of(Format.PATTERN, false, Source.PATH);
        note.setPattern("^[^/]+$");
        MdcRequestFieldsInterceptor interceptor = interceptor(withExtra("note", note));

        interceptor.preHandle(requestWith(Map.of("note", "ok\nINFO fake")), response, new Object());
        assertThat(MDC.get("note")).isNull();

        interceptor.preHandle(requestWith(Map.of("note", "ok")), response, new Object());
        assertThat(MDC.get("note")).isEqualTo("ok");
    }

    @Test
    @DisplayName("an invalid path value is not replaced by a query value of the same name")
    void invalidPathValueNotReplacedByQuery() {
        MdcRequestFieldsInterceptor interceptor =
                interceptor(withExtra("sku", Field.of(Format.CODE, false, Source.PATH, Source.QUERY)));
        MockHttpServletRequest request = requestWith(Map.of("sku", "bad value"));
        request.setQueryString("sku=GOOD");

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("sku")).isNull();
    }

    @Test
    @DisplayName("form body parameters are not read, only the query string")
    void formBodyNotRead() {
        MdcRequestFieldsInterceptor interceptor =
                interceptor(withExtra("sku", Field.of(Format.CODE, false, Source.QUERY)));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.addParameter("sku", "FROM-BODY");

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("sku")).isNull();
    }

    @Test
    @DisplayName("query parsing: first value, URL-decoded, malformed escapes skipped")
    void queryParsing() {
        assertThat(MdcRequestFieldsInterceptor.firstQueryValues("a=1&a=2&b=x%2Dy&c=%ZZ&d"))
                .containsEntry("a", "1")
                .containsEntry("b", "x-y")
                .containsEntry("d", "")
                .doesNotContainKey("c");
    }
}
