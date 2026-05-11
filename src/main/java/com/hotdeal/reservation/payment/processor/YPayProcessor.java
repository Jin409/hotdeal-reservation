package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.payment.client.PgClient;
import com.hotdeal.reservation.payment.client.PgException;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YPayProcessor implements PaymentProcessor {

    private final PgClient pgClient;

    @Override
    @Retryable(
            retryFor = PgException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
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
        return PaymentType.YPAY;
    }
}