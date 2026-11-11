package com.pfm.personalfinancemanager.exception;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

