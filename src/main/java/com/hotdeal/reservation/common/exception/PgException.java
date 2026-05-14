package com.hotdeal.reservation.common.exception;

import lombok.Getter;

@Getter
public class PgException extends RuntimeException {

    private final PgErrorCode errorCode;

    public PgException(PgErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public boolean isRetryable() {
        return errorCode.isRetryable();
    }
}