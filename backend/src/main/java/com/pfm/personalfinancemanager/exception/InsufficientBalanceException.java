package com.pfm.personalfinancemanager.exception;

public class InsufficientBalanceException extends BaseException {

    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE);
    }

    public InsufficientBalanceException(String message) {
        super(ErrorCode.INSUFFICIENT_BALANCE, message);
    }
}

