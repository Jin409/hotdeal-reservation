package com.hotdeal.reservation.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentMethodRequest(
        @NotNull String type,
        @NotNull @Positive long amount
) {
}