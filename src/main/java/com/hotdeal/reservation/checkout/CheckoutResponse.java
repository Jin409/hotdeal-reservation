package com.hotdeal.reservation.checkout;

import java.time.LocalDateTime;

public record CheckoutResponse(
        String productName,
        long price,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        long pointBalance
) {
}