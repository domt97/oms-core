package com.dotran.oms.core.logging;

import org.slf4j.MDC;

import java.util.Map;

/**
 * A unit of work's MDC entries. {@link #close()} restores the MDC exactly as it was when the scope was opened,
 * so scopes nest and pooled threads never keep another request's keys.
 *
 * <pre>
 * try (MdcScope mdc = MdcScope.open()) {
 *     mdc.put(MdcKeys.INVENTORY_ID, id);
 *     try {
 *         ...
 *     } catch (RuntimeException e) {
 *         log.error(...);   // still inside the scope: the line carries inventoryId
 *     }
 * }
 * </pre>
 * Put the {@code catch} inside the scope: a try-with-resources {@code catch} runs after {@code close()}.
 */
public final class MdcScope implements AutoCloseable {

    private final Map<String, String> previous;

    private MdcScope(Map<String, String> previous) {
        this.previous = previous;
    }

    public static MdcScope open() {
        return new MdcScope(MDC.getCopyOfContextMap());
    }

    /** Adds a key; a {@code null} value is skipped. The value is escaped (no control characters). */
    public MdcScope put(String key, Object value) {
        if (key != null && value != null) {
            MDC.put(key, LogSanitizer.escape(String.valueOf(value)));
        }
        return this;
    }

    @Override
    public void close() {
        if (previous == null || previous.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previous);
        }
    }
}
