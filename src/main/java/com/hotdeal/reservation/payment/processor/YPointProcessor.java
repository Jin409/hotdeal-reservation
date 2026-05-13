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
        // 포인트는 외부 PG가 아니므로 취소 불필요
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.YPOINT;
    }
}