package com.hotdeal.reservation.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {

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
}