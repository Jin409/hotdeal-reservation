package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.booking.BookingStatus;
import com.hotdeal.reservation.common.exception.NotFoundException;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private static final long FIRST_IN_LINE = 1;

    private final QueueRedisRepository queueRedisRepository;
    private final BookingRepository bookingRepository;

    public QueueStatusResponse getStatus(Long productId, Long userId) {
        Long rank = queueRedisRepository.getRank(productId, userId);

        if (rank == null) {
            return handleNotInQueue(productId, userId);
        }

        if (rank == FIRST_IN_LINE) {
            return QueueStatusResponse.ready();
        }

        return QueueStatusResponse.waiting(rank);
    }

    private QueueStatusResponse handleNotInQueue(Long productId, Long userId) {
        Booking booking = bookingRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new NotFoundException("대기열 내의 사용자"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return QueueStatusResponse.completed();
        }

        throw new NotFoundException("대기열 내의 사용자");
    }
}
