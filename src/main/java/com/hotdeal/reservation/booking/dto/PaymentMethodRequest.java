package com.hotdeal.reservation.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentMethodRequest(
        @NotNull String type,
        @NotNull @Positive int amount
) {
}