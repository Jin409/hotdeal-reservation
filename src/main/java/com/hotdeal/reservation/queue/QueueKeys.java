package com.hotdeal.reservation.queue;

public class QueueKeys {

    private static final String QUEUE_PREFIX = "queue:product:";
    private static final String ENTERED_PREFIX = "queue:entered:";
    private static final String READY_PREFIX = "queue:ready:";

    public static String queue(Long productId) {
        return QUEUE_PREFIX + productId;
    }

    public static String entered(Long productId) {
        return ENTERED_PREFIX + productId;
    }

    public static String ready(Long productId, Long userId) {
        return READY_PREFIX + productId + ":" + userId;
    }
}