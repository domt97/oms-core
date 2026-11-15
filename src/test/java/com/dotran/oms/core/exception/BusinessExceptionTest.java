package com.dotran.oms.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BusinessException code")
class BusinessExceptionTest {

    static class StaleVersion extends BusinessException {
        StaleVersion(String code) {
            super("The cart changed; reload it", code);
        }
    }

    @Test
    @DisplayName("withCode keeps the message and the code; the old constructors have no code")
    void codes() {
        BusinessException coded = BusinessException.withCode("VERSION_STALE", "The cart changed; reload it");

        assertThat(coded.getMessage()).isEqualTo("The cart changed; reload it");
        assertThat(coded.getCode()).isEqualTo("VERSION_STALE");
        assertThat(new BusinessException("x").getCode()).isNull();
        assertThat(new BusinessException().getCode()).isNull();
        assertThat(new StaleVersion("CART_LOCKED").getCode()).isEqualTo("CART_LOCKED");
        assertThat(BusinessException.withCode("A" + "B".repeat(63), "m").getCode()).hasSize(64);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"version_stale", "1ABC", "_ABC", "VERSION-STALE", "VERSION STALE", "The cart changed; reload it"})
    @DisplayName("a code that is not UPPER_SNAKE (incl. a message passed by mistake) → IllegalArgumentException, from both entry points")
    void invalidCodes(String code) {
        assertThatThrownBy(() -> BusinessException.withCode(code, "m")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StaleVersion(code)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a code longer than 64 characters is refused")
    void tooLong() {
        assertThatThrownBy(() -> BusinessException.withCode("A".repeat(65), "m")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StaleVersion("A".repeat(65))).isInstanceOf(IllegalArgumentException.class);
    }
}
