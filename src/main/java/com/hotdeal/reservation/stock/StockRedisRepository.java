package com.hotdeal.reservation.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public Long decrease(Long productId) {
        return redisTemplate.opsForValue().decrement(StockKeys.stock(productId));
    }

    public void increase(Long productId) {
        redisTemplate.opsForValue().increment(StockKeys.stock(productId));
    }

    public String get(Long productId) {
        return redisTemplate.opsForValue().get(StockKeys.stock(productId));
    }

    public void set(Long productId, int quantity) {
        redisTemplate.opsForValue().set(StockKeys.stock(productId), String.valueOf(quantity));
    }
}