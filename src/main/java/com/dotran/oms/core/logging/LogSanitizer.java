package com.dotran.oms.core.logging;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/** Keeps user-controlled text from forging or splitting log lines. Never throws. */
@UtilityClass
public class LogSanitizer {

    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    // Canonical lowercase/uppercase hex form only; UUID.fromString also accepts "1-2-3-4-5".
    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** An inbound X-Request-Id is kept only if it is short and made of safe characters. */
    public static boolean isValidRequestId(String value) {
        return value != null && REQUEST_ID.matcher(value).matches();
    }

    public static boolean isUuid(String value) {
        return value != null && UUID.matcher(value).matches();
    }

    /**
     * Replaces control characters (CR, LF, TAB, ...) and the Unicode line/paragraph separators with
     * {@code \\uXXXX}, so a value cannot start a new log line. {@code null} stays {@code null}.
     */
    public static String escape(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean unsafe = Character.isISOControl(c) || c == '\u2028' || c == '\u2029';
            if (unsafe) {
                if (sb == null) {
                    sb = new StringBuilder(value.length() + 8).append(value, 0, i);
                }
                sb.append(String.format("\\u%04x", (int) c));
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? value : sb.toString();
    }
}
