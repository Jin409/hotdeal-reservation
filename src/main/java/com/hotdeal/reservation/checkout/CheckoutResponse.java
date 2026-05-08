package com.hotdeal.reservation.checkout;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CheckoutResponse {

    private final String productName;
    private final int price;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
    private final Long pointBalance;

    public CheckoutResponse(String productName, int price, LocalDateTime checkInAt,
                            LocalDateTime checkOutAt, Long pointBalance) {
        this.productName = productName;
        this.price = price;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.pointBalance = pointBalance;
    }
}