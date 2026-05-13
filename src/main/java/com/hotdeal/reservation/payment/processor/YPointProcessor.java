package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.user.User;
import org.springframework.stereotype.Component;

@Component
public class YPointProcessor implements PaymentProcessor {

    @Override
    public void process(String idempotencyKey, User user, long amount) {
        user.usePoints(amount);
    }

    @Override
    public void cancel(String idempotencyKey) {
        throw new UnsupportedOperationException("포인트 결제는 외부 PG 취소 대상이 아닙니다.");
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.YPOINT;
    }
}