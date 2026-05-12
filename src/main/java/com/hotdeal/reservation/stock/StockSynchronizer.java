package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSynchronizer {

    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;

    public void syncAll() {
        productRepository.findAll().forEach(product ->
                stockRedisRepository.set(product.getId(), product.getStock().getQuantity())
        );
        log.info("DB 기준으로 Redis 재고 동기화 완료");
    }
}