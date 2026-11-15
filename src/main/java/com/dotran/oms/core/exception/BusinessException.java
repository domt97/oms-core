package com.dotran.oms.core.exception;

import java.util.regex.Pattern;

/**
 * A business rule was broken (422). It may carry a machine-readable {@link #getCode() code} for clients that must tell
 * several causes apart (sa-api-design rule 9): upper-case letters, digits and {@code _}, starting with a letter,
 * at most 64 characters. Codes are part of the service's API: renaming one is a breaking change.
 */
public class BusinessException extends RuntimeException {

    private static final String ERROR_MESSAGE = "Business Error";
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

    private final String code;

    public BusinessException() {
        super(ERROR_MESSAGE);
        this.code = null;
    }

    public BusinessException(String errorMessage) {
        super(errorMessage);
        this.code = null;
    }

    /** For subclasses: message first, then the code (checked). */
    protected BusinessException(String errorMessage, String code) {
        super(errorMessage);
        this.code = checkCode(code);
    }

    /** A business error with a machine-readable code, e.g. {@code withCode("VERSION_STALE", "…")}. */
    public static BusinessException withCode(String code, String errorMessage) {
        return new BusinessException(errorMessage, code);
    }

    /** The machine-readable code, or {@code null} when the error has none. */
    public String getCode() {
        return code;
    }

    private static String checkCode(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Error code must match " + CODE.pattern());
        }
        return code;
    }
}
