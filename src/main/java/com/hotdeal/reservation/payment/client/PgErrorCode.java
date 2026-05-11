package com.hotdeal.reservation.payment.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PgErrorCode {

    PROVIDER_ERROR(500, "일시적인 오류가 발생했습니다."),
    PG_TIMEOUT(504, "PG사 응답 시간을 초과했습니다."),

    INVALID_REJECT_CARD(400, "카드 사용이 거절되었습니다."),
    INVALID_CARD_EXPIRATION(400, "카드 유효기간이 올바르지 않습니다."),
    INVALID_STOPPED_CARD(400, "정지된 카드입니다."),
    EXCEED_MAX_DAILY_PAYMENT_COUNT(400, "하루 결제 가능 횟수를 초과했습니다."),
    INSUFFICIENT_BALANCE(400, "잔액이 부족합니다.");

    private final int statusCode;
    private final String message;

    public boolean isRetryable() {
        return statusCode >= 500;
    }
}