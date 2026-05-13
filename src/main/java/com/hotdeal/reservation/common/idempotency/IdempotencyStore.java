package com.hotdeal.reservation.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    private static final String PREFIX = "idempotency:";
    private static final String CHECKOUT_PREFIX = "idempotency:checkout:";
    private static final String PROCESSING = "PROCESSING";
    private static final long TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;

    public boolean markProcessing(String key) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(PREFIX + key, PROCESSING, TTL_MINUTES, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(result);
    }

    public void save(String key, String json) {
        redisTemplate.opsForValue().set(PREFIX + key, json, TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Optional<String> find(String key) {
        String value = redisTemplate.opsForValue().get(PREFIX + key);
        return Optional.ofNullable(value);
    }

    public boolean isProcessing(String value) {
        return PROCESSING.equals(value);
    }

    public void delete(String key) {
        redisTemplate.delete(PREFIX + key);
    }

    public String issue(String id) {
        try {
            String checkoutKey = CHECKOUT_PREFIX + id;
            String existing = redisTemplate.opsForValue().get(checkoutKey);
            if (existing != null) {
                return existing;
            }

            String newKey = java.util.UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(checkoutKey, newKey, TTL_MINUTES, TimeUnit.MINUTES);
            return newKey;
        } catch (Exception e) {
            log.warn("멱등키 발급에 실패하여 건너뜁니다.", e);
            return null;
        }
    }
}