package com.dotran.oms.core.common.exception;

public class ImportException extends RuntimeException {

    public ImportException() {
        super("Import failed");
    }

    public ImportException(String message) {
        super(message);
    }
}
