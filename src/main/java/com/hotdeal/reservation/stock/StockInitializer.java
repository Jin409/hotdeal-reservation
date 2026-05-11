package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            productRepository.findAll().forEach(product ->
                    stockRedisRepository.set(product.getId(), product.getStock().getQuantity())
            );
        } catch (Exception e) {
            log.warn("Redis 재고 초기화 실패. Redis 복구 후 재기동이 필요합니다.", e);
        }
    }
}