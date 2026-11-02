package com.dotran.oms.core.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    @DisplayName("isValidRequestId: safe characters up to 64 are kept")
    void validRequestIds() {
        assertThat(LogSanitizer.isValidRequestId("3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab")).isTrue();
        assertThat(LogSanitizer.isValidRequestId("abc.DEF_123-x")).isTrue();
        assertThat(LogSanitizer.isValidRequestId("a".repeat(64))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a b", "a\nb", "a\rb", "<x>", "\u00e9", "a;b"})
    @DisplayName("isValidRequestId: blank, spaces, control, markup, non-ASCII are rejected")
    void invalidRequestIds(String value) {
        assertThat(LogSanitizer.isValidRequestId(value)).isFalse();
    }

    @Test
    @DisplayName("isValidRequestId: null and 65 characters are rejected")
    void nullAndTooLong() {
        assertThat(LogSanitizer.isValidRequestId(null)).isFalse();
        assertThat(LogSanitizer.isValidRequestId("a".repeat(65))).isFalse();
    }

    @Test
    @DisplayName("isUuid: only the canonical 8-4-4-4-12 hex form")
    void isUuid() {
        assertThat(LogSanitizer.isUuid("3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab")).isTrue();
        assertThat(LogSanitizer.isUuid("3F1C2A9E-7D4B-4C1E-9A0B-1234567890AB")).isTrue();
        assertThat(LogSanitizer.isUuid("1-2-3-4-5")).isFalse();
        assertThat(LogSanitizer.isUuid("abc")).isFalse();
        assertThat(LogSanitizer.isUuid("3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab\n")).isFalse();
        assertThat(LogSanitizer.isUuid(null)).isFalse();
    }

    @Test
    @DisplayName("escape: CR, LF, TAB and Unicode line separators become \\uXXXX; other text is unchanged")
    void escape() {
        assertThat(LogSanitizer.escape("a\r\nINFO fake")).isEqualTo("a\\u000d\\u000aINFO fake");
        assertThat(LogSanitizer.escape("x\ty")).isEqualTo("x\\u0009y");
        assertThat(LogSanitizer.escape("p\u2028q\u2029r")).isEqualTo("p\\u2028q\\u2029r");
        assertThat(LogSanitizer.escape("Tr\u00e0 s\u1eefa")).isEqualTo("Tr\u00e0 s\u1eefa");
        assertThat(LogSanitizer.escape(null)).isNull();
    }
}
