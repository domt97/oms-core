package com.dotran.oms.core.logging;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Which request values go into the MDC ({@code app.logging.mdc}). oms-core ships defaults for the common ids;
 * a service adds or overrides entries in its {@code application.yaml}. Spring Boot merges the service's map into
 * these defaults (an overridden key replaces the whole entry).
 *
 * <pre>
 * app:
 *   logging:
 *     mdc:
 *       fields:
 *         sku: { format: code, sources: [path, query] }
 *         orderNo: { format: pattern, pattern: "^ORD-[0-9]{1,12}$" }
 * </pre>
 *
 * Every value is validated: one that does not match its format, or contains control characters, is not logged. The
 * configuration is checked at startup: a broken entry fails the application instead of silently dropping a key.
 * Never configure personal data (phone, email, names). Query values are chosen by the caller on any route: never use
 * {@code query} for tenant ids. Values only known in code (request bodies, event payloads) are added with
 * {@link MdcScope}. Keys in {@link #RESERVED_KEYS} only take {@code propagate}.
 */
@Configuration
@ConfigurationProperties(prefix = "app.logging.mdc")
@Getter
@Setter
public class MdcProperties {

    /**
     * Values from {@link Format#PATTERN} entries longer than this are never logged. Short on purpose: the regex runs
     * on caller input, so keep patterns linear (no nested quantifiers such as {@code (a+)+}).
     */
    public static final int MAX_PATTERN_VALUE_LENGTH = 64;

    /**
     * Keys written by oms-core or the messaging code, never from a request value: they may appear here only to set
     * {@code propagate}, with no sources. {@code eventId} is the cross-service join key, so a caller must not set it.
     */
    public static final Set<String> RESERVED_KEYS = Set.of(MdcKeys.REQUEST_ID, MdcKeys.EVENT_ID, MdcKeys.KAFKA_TOPIC,
            MdcKeys.KAFKA_PARTITION, MdcKeys.KAFKA_OFFSET, MdcKeys.SQS_MESSAGE_ID);

    private static final Pattern KEY_NAME = Pattern.compile("^[a-zA-Z][A-Za-z0-9]{0,40}$");

    private Map<String, Field> fields = defaults();

    // Precomputed by validate(): read on every request.
    private Map<String, Field> pathFields = Map.of();
    private Map<String, Field> queryFields = Map.of();

    public enum Format {
        /** Canonical 8-4-4-4-12 hex UUID. */
        UUID,
        /** Letters, digits, {@code . _ -}; 1 to 64 characters (merchant codes, SKUs, request ids). */
        CODE,
        /** The entry's own {@code pattern}, on values up to {@value #MAX_PATTERN_VALUE_LENGTH} characters. */
        PATTERN
    }

    public enum Source {
        /** URI template variable of the matched route. */
        PATH,
        /**
         * Query string parameter (first value; form bodies are not read). The caller chooses this value on any route,
         * so use it only for values that do not identify a tenant or an event (never merchantId, eventId).
         */
        QUERY
    }

    @Getter
    @Setter
    public static class Field {

        private Format format = Format.UUID;
        private String pattern;
        private Set<Source> sources = EnumSet.of(Source.PATH);
        /**
         * Marks a key that may be carried to other services (outbox → Kafka header, outbound HTTP). Only the
         * marking exists today; the transport is a follow-up (oms-workspace open question 46). Propagate correlation
         * keys only, not entity ids an event payload already carries.
         */
        private boolean propagate;

        @Getter(lombok.AccessLevel.NONE)
        @Setter(lombok.AccessLevel.NONE)
        private Pattern compiled;

        public static Field of(Format format, boolean propagate, Source... sources) {
            Field field = new Field();
            field.setFormat(format);
            field.setPropagate(propagate);
            field.setSources(sources.length == 0 ? EnumSet.noneOf(Source.class) : EnumSet.of(sources[0], sources));
            return field;
        }

        /** True when the value may be logged under this field. Never throws. */
        public boolean accepts(String value) {
            // Whatever the format, a value with control characters or line separators is never logged as is.
            if (value == null || format == null || !LogSanitizer.escape(value).equals(value)) {
                return false;
            }
            return switch (format) {
                case UUID -> LogSanitizer.isUuid(value);
                case CODE -> LogSanitizer.isValidRequestId(value);
                case PATTERN -> compiled != null && value.length() <= MAX_PATTERN_VALUE_LENGTH
                        && compiled.matcher(value).matches();
            };
        }

        void validate(String key) {
            if (format == null) {
                throw invalid(key, "format is required (uuid, code or pattern)");
            }
            if (format != Format.PATTERN && pattern != null) {
                throw invalid(key, "'pattern' is only used with format: pattern");
            }
            if (format == Format.PATTERN) {
                if (pattern == null || pattern.isBlank()) {
                    throw invalid(key, "format 'pattern' needs a 'pattern'");
                }
                try {
                    compiled = Pattern.compile(pattern);
                } catch (PatternSyntaxException e) {
                    throw invalid(key, "invalid pattern: " + e.getDescription());
                }
            }
            if (sources == null) {
                sources = EnumSet.noneOf(Source.class);
            }
        }
    }

    @PostConstruct
    public void validate() {
        if (fields == null) {
            fields = defaults();
        }
        Map<String, String> lowerCaseKeys = new java.util.HashMap<>();
        fields.forEach((key, field) -> {
            if (key == null || !KEY_NAME.matcher(key).matches()) {
                throw invalid(key, "key must be letters and digits, starting with a letter (max 41)");
            }
            // Environment variables lowercase map keys (APP_LOGGING_MDC_FIELDS_MERCHANTID -> "merchantid"): that would
            // add a key no path variable matches. Use SPRING_APPLICATION_JSON or YAML for camelCase keys.
            String previous = lowerCaseKeys.put(key.toLowerCase(java.util.Locale.ROOT), key);
            if (previous != null) {
                throw invalid(key, "differs from '" + previous + "' only by case (set from an environment variable?)");
            }
            if (field == null) {
                throw invalid(key, "entry is empty");
            }
            field.validate(key);
            if (RESERVED_KEYS.contains(key) && !field.getSources().isEmpty()) {
                throw invalid(key, "is written by oms-core or the message listeners, never from a request; "
                        + "set 'sources: []' (only 'propagate' may be configured)");
            }
        });
        RESERVED_KEYS.forEach(reserved -> {
            String sameIgnoringCase = lowerCaseKeys.get(reserved.toLowerCase(java.util.Locale.ROOT));
            if (sameIgnoringCase != null && !sameIgnoringCase.equals(reserved)) {
                throw invalid(sameIgnoringCase, "looks like '" + reserved + "' with a different case");
            }
        });
        pathFields = collect(Source.PATH);
        queryFields = collect(Source.QUERY);
    }

    /** Fields read from the given source (computed at startup). */
    public Map<String, Field> fieldsFrom(Source source) {
        return source == Source.PATH ? pathFields : queryFields;
    }

    private Map<String, Field> collect(Source source) {
        Map<String, Field> result = new LinkedHashMap<>();
        fields.forEach((key, field) -> {
            if (field.getSources().contains(source) && !RESERVED_KEYS.contains(key)) {
                result.put(key, field);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /** Keys marked {@code propagate: true} (transport is a follow-up). */
    public Set<String> propagatedKeys() {
        Set<String> keys = new TreeSet<>();
        fields.forEach((key, field) -> {
            if (field.isPropagate()) {
                keys.add(key);
            }
        });
        return Collections.unmodifiableSet(keys);
    }

    static Map<String, Field> defaults() {
        Map<String, Field> defaults = new LinkedHashMap<>();
        defaults.put(MdcKeys.REQUEST_ID, Field.of(Format.CODE, true));
        defaults.put(MdcKeys.MERCHANT_ID, Field.of(Format.UUID, false, Source.PATH));
        defaults.put(MdcKeys.STORE_ID, Field.of(Format.UUID, false, Source.PATH));
        defaults.put(MdcKeys.PRODUCT_ID, Field.of(Format.UUID, false, Source.PATH));
        defaults.put(MdcKeys.INVENTORY_ID, Field.of(Format.UUID, false, Source.PATH));
        defaults.put(MdcKeys.COLLECTION_ID, Field.of(Format.UUID, false, Source.PATH));
        defaults.put(MdcKeys.STORE_AVAILABILITY_ID, Field.of(Format.UUID, false, Source.PATH));
        return defaults;
    }

    private static IllegalStateException invalid(String key, String reason) {
        return new IllegalStateException("Invalid app.logging.mdc.fields." + key + ": " + reason);
    }
}
