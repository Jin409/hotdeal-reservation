package com.hotdeal.reservation.booking.dto;

import com.hotdeal.reservation.payment.PaymentMethodRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookingRequest(
        @NotNull Long productId,
        @NotEmpty @Valid List<PaymentMethodRequest> paymentMethods
) {
}