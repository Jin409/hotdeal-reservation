package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.common.exception.NotFoundException;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueStatusService {

    private final QueueRedisRepository queueRedisRepository;

    public QueueStatusResponse getStatus(Long productId, Long userId) {
        Long rank = queueRedisRepository.getRank(productId, userId);

        if (rank == null) {
            throw new NotFoundException("대기열");
        }

        if (rank == 1) {
            return QueueStatusResponse.ready();
        }

        return QueueStatusResponse.waiting(rank);
    }
}
