package com.hotdeal.reservation.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthChecker {

    private final StringRedisTemplate redisTemplate;
    private final StockSynchronizer stockSynchronizer;

    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5_000)
    public void check() {
        if (isAlive()) {
            handleRecovery();
        } else {
            handleFailure();
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable.get() && !syncing.get();
    }

    private boolean isAlive() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleRecovery() {
        if (redisAvailable.get()) {
            return;
        }

        log.info("Redis 복구 감지. 재고 재동기화를 시작합니다.");
        syncing.set(true);
        try {
            stockSynchronizer.syncAll();
        } finally {
            syncing.set(false);
        }
        redisAvailable.set(true);
    }

    private void handleFailure() {
        if (!redisAvailable.get()) {
            return;
        }

        log.warn("Redis 장애 감지.");
        redisAvailable.set(false);
    }
}