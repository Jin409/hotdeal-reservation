package com.hotdeal.reservation.payment.client;

public interface PgClient {

    void charge(String idempotencyKey, long amount);
}