package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.payment.client.PgClient;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YPayProcessor implements PaymentProcessor {

    private final PgClient pgClient;

    @Override
    public void process(String idempotencyKey, User user, long amount) {
        pgClient.charge(idempotencyKey, amount);
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.YPAY;
    }
}