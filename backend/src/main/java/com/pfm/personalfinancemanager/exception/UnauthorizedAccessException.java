package com.pfm.personalfinancemanager.exception;

public class UnauthorizedAccessException extends BaseException {

    public UnauthorizedAccessException() {
        super(ErrorCode.ACCESS_DENIED_OWNERSHIP);
    }

    public UnauthorizedAccessException(String message) {
        super(ErrorCode.ACCESS_DENIED_OWNERSHIP, message);
    }

    public UnauthorizedAccessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

