package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.payment.client.PgClient;
import com.hotdeal.reservation.payment.client.PgException;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditCardProcessor implements PaymentProcessor {

    private final PgClient pgClient;

    @Override
    public void process(String idempotencyKey, User user, long amount) {
        try {
            pgClient.charge(idempotencyKey, amount);
        } catch (PgException e) {
            if (!e.isRetryable()) {
                throw new BadRequestException(e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.CREDIT_CARD;
    }
}