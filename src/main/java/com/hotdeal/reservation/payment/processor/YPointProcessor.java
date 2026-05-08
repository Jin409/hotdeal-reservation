package com.hotdeal.reservation.payment.processor;

import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.user.User;
import org.springframework.stereotype.Component;

@Component
public class YPointProcessor implements PaymentProcessor {

    @Override
    public void process(User user, int amount) {
        user.usePoints(amount);
    }

    @Override
    public PaymentType supportedType() {
        return PaymentType.YPOINT;
    }
}