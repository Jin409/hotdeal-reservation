package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.user.User;

public interface PaymentProcessor {

    void process(User user, long amount);

    PaymentType supportedType();
}