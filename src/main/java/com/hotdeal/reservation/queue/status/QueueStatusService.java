package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.booking.BookingStatus;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.common.exception.NotFoundException;
import com.hotdeal.reservation.common.exception.ServiceUnavailableException;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private static final long FIRST_IN_LINE = 1;

    private final QueueRedisRepository queueRedisRepository;
    private final BookingRepository bookingRepository;
    private final QueueService queueService;
    private final StockRedisRepository stockRedisRepository;

    public QueueStatusResponse getStatus(Long productId, Long userId) {
        Long rank = getRank(productId, userId);

        if (rank == null) {
            return handleNotInQueue(productId, userId);
        }

        if (rank == FIRST_IN_LINE) {
            return QueueStatusResponse.ready();
        }

        return QueueStatusResponse.waiting(rank);
    }

    private Long getRank(Long productId, Long userId) {
        try {
            return queueRedisRepository.getRank(productId, userId);
        } catch (Exception e) {
            log.warn("Redis 장애로 대기열 순번 조회를 할 수 없습니다.", e);
            throw new ServiceUnavailableException("대기열 서비스가 일시적으로 불안정합니다.");
        }
    }

    private QueueStatusResponse handleNotInQueue(Long productId, Long userId) {
        Booking booking = bookingRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new NotFoundException("대기열 내의 사용자"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return QueueStatusResponse.completed();
        }

        return reEnter(productId, userId);
    }

    private QueueStatusResponse reEnter(Long productId, Long userId) {
        if (!hasStock(productId)) {
            throw new BadRequestException("재고가 없습니다.");
        }

        queueService.removeEntry(productId, userId);
        Long newRank = queueService.enter(productId, userId);
        return QueueStatusResponse.waiting(newRank);
    }

    private boolean hasStock(Long productId) {
        String stock = stockRedisRepository.get(productId);
        return stock != null && Long.parseLong(stock) > 0;
    }
}