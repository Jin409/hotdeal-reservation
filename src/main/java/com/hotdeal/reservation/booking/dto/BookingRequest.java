package com.hotdeal.reservation.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BookingRequest {

    @NotNull
    private Long productId;

    @NotEmpty
    @Valid
    private List<PaymentMethodRequest> paymentMethods;

    public BookingRequest(Long productId, List<PaymentMethodRequest> paymentMethods) {
        this.productId = productId;
        this.paymentMethods = paymentMethods;
    }
}