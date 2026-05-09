package com.hotdeal.reservation.stock;

public class StockKeys {

    private static final String STOCK_PREFIX = "stock:product:";

    public static String stock(Long productId) {
        return STOCK_PREFIX + productId;
    }
}
