package com.hotdeal.reservation.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentMethodRequest {

    @NotNull
    private String type;

    @NotNull
    @Positive
    private int amount;

    public PaymentMethodRequest(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}