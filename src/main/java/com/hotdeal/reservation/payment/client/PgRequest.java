package com.hotdeal.reservation.payment.client;

import com.hotdeal.reservation.payment.PaymentType;
import lombok.Getter;

@Getter
public class PgRequest {

    private final PaymentType paymentType;
    private final int amount;

    public PgRequest(PaymentType paymentType, int amount) {
        this.paymentType = paymentType;
        this.amount = amount;
    }
}