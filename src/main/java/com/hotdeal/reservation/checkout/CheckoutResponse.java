package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.user.User;

import java.time.LocalDateTime;

public record CheckoutResponse(
        Long bookingId,
        String idempotencyKey,
        String productName,
        long price,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        long pointBalance,
        Long rank
) {
    public static CheckoutResponse of(Product product, User user, Long rank, Long bookingId, String idempotencyKey) {
        return new CheckoutResponse(
                bookingId,
                idempotencyKey,
                product.getName(),
                product.getPrice(),
                product.getCheckInAt(),
                product.getCheckOutAt(),
                user.getPointBalance(),
                rank
        );
    }
}
