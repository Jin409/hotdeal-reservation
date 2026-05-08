package com.hotdeal.reservation.queue;

public class QueueKeys {

    public static String queue(Long productId) {
        return "queue:product:" + productId;
    }

    public static String entered(Long productId) {
        return "queue:entered:" + productId;
    }
}