package com.hotdeal.reservation.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    private static final String PREFIX = "idempotency:";
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
}