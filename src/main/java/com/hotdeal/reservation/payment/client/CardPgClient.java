package com.hotdeal.reservation.payment.client;

public interface CardPgClient {

    void charge(String idempotencyKey, long amount);
}