package com.pfm.personalfinancemanager.exception;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                String.format("%s với %s '%s' đã tồn tại", resourceName, fieldName, fieldValue));
    }
}

