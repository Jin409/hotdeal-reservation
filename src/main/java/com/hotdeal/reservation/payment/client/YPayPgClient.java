package com.hotdeal.reservation.payment.client;

public interface YPayPgClient {

    void charge(String idempotencyKey, long amount);
}