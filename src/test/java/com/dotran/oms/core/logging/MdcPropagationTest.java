package com.dotran.oms.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MdcPropagationTest {

    private final MdcPropagation propagation = new MdcPropagation(defaults());

    private static MdcProperties defaults() {
        MdcProperties properties = new MdcProperties();
        properties.validate();
        return properties;
    }

    private static MdcProperties withTraceId() {
        MdcProperties properties = new MdcProperties();
        properties.getFields().put("traceId",
                MdcProperties.Field.of(MdcProperties.Format.CODE, true));
        properties.validate();
        return properties;
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    @DisplayName("capture: empty MDC gives an empty map, never null")
    void captureEmpty() {
        assertThat(propagation.capture()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("capture: only propagated keys with valid values")
    void captureOnlyPropagated() {
        MDC.put("requestId", "abc-123");
        MDC.put("storeId", "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");
        MDC.put("eventId", "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");

        assertThat(propagation.capture()).containsExactly(Map.entry("requestId", "abc-123"));
    }

    @Test
    @DisplayName("capture: a value failing its format is skipped")
    void captureSkipsInvalid() {
        MDC.put("requestId", "bad id!");

        assertThat(propagation.capture()).isEmpty();
    }

    @Test
    @DisplayName("capture/restore: a service-defined correlation key travels too")
    void customCorrelationKey() {
        MdcPropagation withTrace = new MdcPropagation(withTraceId());
        MDC.put("requestId", "r1");
        MDC.put("traceId", "t-1");

        assertThat(withTrace.capture()).containsOnly(Map.entry("requestId", "r1"), Map.entry("traceId", "t-1"));
        assertThat(withTrace.restore(utf8("{\"requestId\":\"r1\",\"traceId\":\"t-1\"}")))
                .containsOnly(Map.entry("requestId", "r1"), Map.entry("traceId", "t-1"));
    }

    @Test
    @DisplayName("restore: a valid requestId is kept")
    void restoreValid() {
        assertThat(propagation.restore(utf8("{\"requestId\":\"e2e-q46-1\"}")))
                .containsExactly(Map.entry("requestId", "e2e-q46-1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "not json", "[1]", "\"text\"", "null", "{\"requestId\":1}",
            "{\"requestId\":null}", "{\"requestId\":{\"a\":1}}", "{\"requestId\":\"a b\"}",
            "{\"requestId\":\"a\\nb\"}", "{\"requestId\":\"a\\u2028b\"}",
            "{\"requestId\":\"x\",\"requestId\":\"y\"}"})
    @DisplayName("restore: empty, malformed, wrongly typed, invalid or duplicate values give an empty map")
    void restoreRejects(String header) {
        assertThat(propagation.restore(utf8(header))).isEmpty();
    }

    @Test
    @DisplayName("restore: null bytes, a 65-character id and invalid UTF-8 give an empty map")
    void restoreRejectsOther() {
        assertThat(propagation.restore(null)).isEmpty();
        assertThat(propagation.restore(utf8("{\"requestId\":\"" + "a".repeat(65) + "\"}"))).isEmpty();
        byte[] badUtf8Value = {'{', '"', 'r', 'e', 'q', 'u', 'e', 's', 't', 'I', 'd', '"', ':', '"', 'a', (byte) 0xC3, (byte) 0x28, '"', '}'};
        assertThat(propagation.restore(badUtf8Value)).isEmpty();
        assertThat(propagation.restore(utf8("{\"requestId\":\"a\"} junk"))).isEmpty();
    }

    @Test
    @DisplayName("restore: keys this service does not propagate, and message or entity keys, are dropped")
    void restoreDropsForeignKeys() {
        Map<String, String> restored = propagation.restore(utf8(
                "{\"requestId\":\"r1\",\"eventId\":\"3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab\",\"kafkaOffset\":\"7\","
                        + "\"merchantId\":\"3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab\",\"traceId\":\"t\"}"));

        assertThat(restored).containsExactly(Map.entry("requestId", "r1"));
    }

    @Test
    @DisplayName("restore: the size cap is 1024 bytes")
    void restoreSizeCap() {
        String prefix = "{\"requestId\":\"r1\",\"pad\":\"";
        String suffix = "\"}";
        String atLimit = prefix + "p".repeat(MdcPropagation.MAX_HEADER_BYTES - prefix.length() - suffix.length()) + suffix;
        String overLimit = prefix + "p".repeat(MdcPropagation.MAX_HEADER_BYTES - prefix.length() - suffix.length() + 1) + suffix;

        assertThat(utf8(atLimit)).hasSize(MdcPropagation.MAX_HEADER_BYTES);
        assertThat(propagation.restore(utf8(atLimit))).containsExactly(Map.entry("requestId", "r1"));
        assertThat(propagation.restore(utf8(overLimit))).isEmpty();
    }

    @Test
    @DisplayName("restore never throws on arbitrary bytes")
    void restoreNeverThrows() {
        java.util.Random random = new java.util.Random(46);
        for (int i = 0; i < 500; i++) {
            byte[] bytes = new byte[random.nextInt(64)];
            random.nextBytes(bytes);
            assertThatCode(() -> propagation.restore(bytes)).doesNotThrowAnyException();
        }
    }
}
