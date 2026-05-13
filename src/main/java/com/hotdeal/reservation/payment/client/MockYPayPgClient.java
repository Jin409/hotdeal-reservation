package com.hotdeal.reservation.payment.client;

import org.springframework.stereotype.Component;

@Component
public class MockYPayPgClient implements YPayPgClient {

    @Override
    public void charge(String idempotencyKey, long amount) {
    }

    @Override
    public void cancel(String idempotencyKey) {
    }
}