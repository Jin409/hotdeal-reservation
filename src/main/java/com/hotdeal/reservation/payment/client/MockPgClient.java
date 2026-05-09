package com.hotdeal.reservation.payment.client;

import org.springframework.stereotype.Component;

@Component
public class MockPgClient implements PgClient {

    @Override
    public void charge(String idempotencyKey, long amount) {
    }
}