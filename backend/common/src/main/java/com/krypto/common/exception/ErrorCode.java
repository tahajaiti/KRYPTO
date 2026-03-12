package com.krypto.common.exception;

public enum ErrorCode {

    INTERNAL_ERROR("An unexpected error occurred"),
    RESOURCE_NOT_FOUND("Resource not found"),
    VALIDATION_FAILED("Validation failed"),
    DUPLICATE_RESOURCE("Resource already exists"),
    UNAUTHORIZED("Authentication required"),
    FORBIDDEN("Access denied"),
    INSUFFICIENT_BALANCE("Insufficient balance"),
    INVALID_TRANSACTION("Invalid transaction"),
    SERVICE_UNAVAILABLE("Service temporarily unavailable");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
