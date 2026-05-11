package com.hotdeal.reservation.queue.status;

public record QueueStatusResponse(
        QueueStatus status,
        Long rank
) {
    public static QueueStatusResponse waiting(Long rank) {
        return new QueueStatusResponse(QueueStatus.WAITING, rank);
    }

    public static QueueStatusResponse ready() {
        return new QueueStatusResponse(QueueStatus.READY, 1L);
    }

    public static QueueStatusResponse completed() {
        return new QueueStatusResponse(QueueStatus.COMPLETED, null);
    }
}
