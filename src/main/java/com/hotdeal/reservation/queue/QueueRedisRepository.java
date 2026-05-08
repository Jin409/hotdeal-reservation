package com.hotdeal.reservation.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public boolean tryEnter(Long productId, Long userId) {
        Long added = redisTemplate.opsForSet().add("queue:entered:" + productId, userId.toString());
        return added != null && added > 0;
    }

    public void addToQueue(Long productId, Long userId) {
        redisTemplate.opsForList().rightPush("queue:product:" + productId, userId.toString());
    }

    public Long getRank(Long productId, Long userId) {
        Long index = redisTemplate.opsForList().indexOf("queue:product:" + productId, userId.toString());
        if (index == null) {
            return null;
        }
        return index + 1;
    }
}