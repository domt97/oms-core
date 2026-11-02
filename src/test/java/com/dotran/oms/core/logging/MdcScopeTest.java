package com.dotran.oms.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MdcScopeTest {

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    @DisplayName("close() on an empty MDC leaves it empty")
    void closeClears() {
        try (MdcScope mdc = MdcScope.open()) {
            mdc.put("requestId", "r1");
            assertThat(MDC.get("requestId")).isEqualTo("r1");
        }
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("nested scope restores the outer keys, including overwritten ones")
    void nested() {
        try (MdcScope outer = MdcScope.open()) {
            outer.put("requestId", "outer").put("storeId", "s1");
            try (MdcScope inner = MdcScope.open()) {
                inner.put("requestId", "inner").put("inventoryId", "i1");
                assertThat(MDC.get("requestId")).isEqualTo("inner");
            }
            assertThat(MDC.get("requestId")).isEqualTo("outer");
            assertThat(MDC.get("storeId")).isEqualTo("s1");
            assertThat(MDC.get("inventoryId")).isNull();
        }
    }

    @Test
    @DisplayName("restores after an exception")
    void restoresOnException() {
        MDC.put("before", "x");
        assertThatThrownBy(() -> {
            try (MdcScope mdc = MdcScope.open()) {
                mdc.put("requestId", "r1");
                throw new IllegalStateException("boom");
            }
        }).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("before")).isEqualTo("x");
    }

    @Test
    @DisplayName("null values are skipped and values are escaped")
    void nullSkippedAndEscaped() {
        try (MdcScope mdc = MdcScope.open()) {
            mdc.put("a", null).put("b", "x\ny");
            assertThat(MDC.get("a")).isNull();
            assertThat(MDC.get("b")).isEqualTo("x\\u000ay");
        }
    }
}
