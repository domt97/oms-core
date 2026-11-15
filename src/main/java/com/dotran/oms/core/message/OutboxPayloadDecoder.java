package com.dotran.oms.core.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads an outbox event from a Kafka record value. The Debezium EventRouter publishes the payload as a JSON string that
 * contains the event JSON (the Connect JSON converter encodes it twice); a plain JSON object is accepted too, so a
 * connector change does not break consumers.
 * <p>
 * A null, empty or non-object value throws a {@link JsonProcessingException}, which consumers must treat as a contract error
 * (configure the listener's error handler to send it to the dead-letter topic at once, not retry it). Trailing tokens and unknown properties follow the given mapper (with Spring
 * Boot's mapper: trailing content after the first value is ignored, unknown properties are allowed).
 * <p>
 * Not a Spring bean: a service declares it ({@code @Bean OutboxPayloadDecoder outboxPayloadDecoder(ObjectMapper m)}).
 */
public class OutboxPayloadDecoder {

    private final ObjectMapper objectMapper;

    public OutboxPayloadDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T decode(String value, Class<T> type) throws JsonProcessingException {
        if (value == null || value.isBlank()) {
            throw new JsonMappingException(null, "Event payload is empty");
        }
        JsonNode node = objectMapper.readTree(value);
        if (node != null && node.isTextual()) {
            node = objectMapper.readTree(node.asText());
        }
        if (node == null || !node.isObject()) {
            throw new JsonMappingException(null, "Event payload is not a JSON object");
        }
        return objectMapper.treeToValue(node, type);
    }
}
