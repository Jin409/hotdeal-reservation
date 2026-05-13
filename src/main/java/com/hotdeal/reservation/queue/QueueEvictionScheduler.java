package com.hotdeal.reservation.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueEvictionScheduler {

    private final QueueService queueService;
    private final QueueRedisRepository queueRedisRepository;

    @Scheduled(fixedDelay = 5_000)
    public void evict() {
        try {
            Set<Long> productIds = queueRedisRepository.getActiveProductIds();
            productIds.forEach(queueService::evictReadyExpiredUser);
        } catch (Exception e) {
            log.warn("대기열 만료 사용자 정리에 실패했습니다.", e);
        }
    }
}