package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.payment.client.CardPgClient;
import com.hotdeal.reservation.common.exception.PgException;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditCardProcessor implements PaymentProcessor {

    private final CardPgClient cardPgClient;

    @Override
    @Retryable(
            retryFor = PgException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void process(String idempotencyKey, User user, long amount) {
        try {
            cardPgClient.charge(idempotencyKey, amount);
        } catch (PgException e) {
            if (!e.isRetryable()) {
                throw new BadRequestException(e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public void cancel(String idempotencyKey) {
        cardPgClient.cancel(idempotencyKey);
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.CREDIT_CARD;
    }
}