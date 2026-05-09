package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRedisRepository stockRedisRepository;

    public void decrease(Long productId) {
        Long remaining = stockRedisRepository.decrease(productId);
        if (remaining < 0) {
            stockRedisRepository.increase(productId);
            throw new BadRequestException("재고가 없습니다.");
        }
    }

    public void rollback(Long productId) {
        stockRedisRepository.increase(productId);
    }
}