package com.hotdeal.reservation.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRedisRepository queueRedisRepository;

    public Long enter(Long productId, Long userId) {
        boolean entered = queueRedisRepository.tryEnter(productId, userId);
        if (!entered) {
            throw new DuplicateEntryException("이미 대기열에 진입한 사용자입니다.");
        }

        queueRedisRepository.addToQueue(productId, userId);
        return queueRedisRepository.getRank(productId, userId);
    }
}