package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.user.User;

import java.time.LocalDateTime;

public record CheckoutResponse(
        String productName,
        long price,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        long pointBalance,
        Long rank
) {
    public static CheckoutResponse of(Product product, User user, Long rank) {
        return new CheckoutResponse(
                product.getName(),
                product.getPrice(),
                product.getCheckInAt(),
                product.getCheckOutAt(),
                user.getPointBalance(),
                rank
        );
    }
}