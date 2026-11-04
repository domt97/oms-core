package com.dotran.oms.core.logging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries the correlation keys marked {@code propagate: true} in {@code app.logging.mdc.fields} from one service to
 * another (ADR 0004). The producer stores {@link #capture()} with the outbox row; Debezium puts it in the Kafka header
 * {@value #HEADER} as JSON text; the consumer puts {@link #restore(byte[])} into its MDC.
 * <p>
 * The header comes from an internal topic but is still untrusted: {@code restore} keeps only keys this service
 * propagates itself, never a message key ({@code eventId}, {@code kafka*}, {@code sqsMessageId}), and only values
 * that pass the key's format. It never throws: a broken header must not send a valid event to the dead-letter topic.
 */
@Component
@Slf4j
public class MdcPropagation {

    /** Kafka header name (also the outbox column, as {@code correlation_context}). */
    public static final String HEADER = "correlationContext";

    /** Larger headers are ignored without parsing. */
    public static final int MAX_HEADER_BYTES = 1024;

    // Private mapper: independent of each service's Jackson customisations; duplicate keys are rejected.
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final MdcProperties properties;

    public MdcPropagation(MdcProperties properties) {
        this.properties = properties;
    }

    /** The propagated keys currently in the MDC whose values pass their format. Never null; may be empty. */
    public Map<String, String> capture() {
        Map<String, String> captured = new LinkedHashMap<>();
        for (String key : properties.propagatedKeys()) {
            String value = MDC.get(key);
            MdcProperties.Field field = properties.getFields().get(key);
            if (field != null && field.accepts(value)) {
                captured.put(key, value);
            }
        }
        return Collections.unmodifiableMap(captured);
    }

    /** The valid propagated keys of a {@value #HEADER} header value (UTF-8 JSON object). Never null; never throws. */
    public Map<String, String> restore(byte[] header) {
        if (header == null || header.length == 0) {
            return Map.of();
        }
        if (header.length > MAX_HEADER_BYTES) {
            log.warn("Ignoring {} header: {} bytes (max {})", HEADER, header.length, MAX_HEADER_BYTES);
            return Map.of();
        }
        JsonNode root;
        try {
            root = JSON.readTree(header);
        } catch (Exception e) {
            // Never log the value: it is caller-controlled.
            log.warn("Ignoring {} header: not valid JSON", HEADER);
            return Map.of();
        }
        if (root == null || !root.isObject()) {
            log.warn("Ignoring {} header: not a JSON object", HEADER);
            return Map.of();
        }
        Map<String, String> restored = new LinkedHashMap<>();
        for (String key : properties.propagatedKeys()) {
            JsonNode node = root.get(key);
            MdcProperties.Field field = properties.getFields().get(key);
            if (node != null && node.isTextual() && field != null && !MdcProperties.MESSAGE_KEYS.contains(key)
                    && field.accepts(node.textValue())) {
                restored.put(key, node.textValue());
            }
        }
        return Collections.unmodifiableMap(restored);
    }
}
