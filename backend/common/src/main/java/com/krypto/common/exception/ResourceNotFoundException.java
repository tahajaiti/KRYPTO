package com.krypto.common.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                resourceName + " not found with identifier: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
