package com.dotran.oms.core.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OutboxPayloadDecoder")
class OutboxPayloadDecoderTest {

    record Sample(String id, int quantity, String note) {
    }

    /** What a service injects: Spring Boot's mapper is built from this builder. */
    private final OutboxPayloadDecoder decoder = new OutboxPayloadDecoder(Jackson2ObjectMapperBuilder.json().build());

    @Test
    @DisplayName("reads the EventRouter form: a JSON string that contains the event JSON")
    void stringWrapped() throws Exception {
        String wrapped = new ObjectMapper().writeValueAsString("{\"id\":\"a\",\"quantity\":2,\"note\":\"{x}\"}");

        assertThat(decoder.decode(wrapped, Sample.class)).isEqualTo(new Sample("a", 2, "{x}"));
    }

    @Test
    @DisplayName("reads a plain JSON object too; unknown properties are ignored by Boot's mapper")
    void plainObject() throws Exception {
        assertThat(decoder.decode("{\"id\":\"a\",\"quantity\":2,\"extra\":true}", Sample.class))
                .isEqualTo(new Sample("a", 2, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not json", "42", "[1,2]", "\"null\"", "null", "\"\""})
    @DisplayName("null, empty, not JSON, not an object (also inside the string) → JsonProcessingException, never IllegalArgumentException")
    void notAnObject(String value) {
        assertThatThrownBy(() -> decoder.decode(value, Sample.class)).isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("null, empty and blank are reported as a contract error (JsonMappingException)")
    void nullAndBlankAreMappingErrors(String value) {
        assertThatThrownBy(() -> decoder.decode(value, Sample.class)).isInstanceOf(JsonMappingException.class);
    }

    @Test
    @DisplayName("trailing tokens after the object follow the mapper: Boot's default ignores them")
    void trailingTokens() throws Exception {
        assertThat(decoder.decode("{\"id\":\"a\",\"quantity\":1} trailing", Sample.class))
                .isEqualTo(new Sample("a", 1, null));
    }
}
