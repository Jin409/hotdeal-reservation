package com.hotdeal.reservation.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {

    private static final long READY_TTL_MINUTES = 3;

    private final StringRedisTemplate redisTemplate;

    public boolean tryEnter(Long productId, Long userId) {
        Long added = redisTemplate.opsForSet().add(QueueKeys.entered(productId), userId.toString());
        return added != null && added > 0;
    }

    public void addToQueue(Long productId, Long userId) {
        redisTemplate.opsForList().rightPush(QueueKeys.queue(productId), userId.toString());
    }

    public void removeEntry(Long productId, Long userId) {
        redisTemplate.opsForSet().remove(QueueKeys.entered(productId), userId.toString());
    }

    public void popFirst(Long productId) {
        redisTemplate.opsForList().leftPop(QueueKeys.queue(productId));
    }

    public Long getRank(Long productId, Long userId) {
        Long index = redisTemplate.opsForList().indexOf(QueueKeys.queue(productId), userId.toString());
        if (index == null) {
            return null;
        }
        return index + 1;
    }

    public void markReady(Long productId, Long userId) {
        redisTemplate.opsForValue().set(
                QueueKeys.ready(productId, userId), "READY", READY_TTL_MINUTES, TimeUnit.MINUTES
        );
    }

    public boolean isReady(Long productId, Long userId) {
        return redisTemplate.hasKey(QueueKeys.ready(productId, userId));
    }

    public void removeReady(Long productId, Long userId) {
        redisTemplate.delete(QueueKeys.ready(productId, userId));
    }

    public Long getFirstUserId(Long productId) {
        String userId = redisTemplate.opsForList().index(QueueKeys.queue(productId), 0);
        return userId != null ? Long.parseLong(userId) : null;
    }
}