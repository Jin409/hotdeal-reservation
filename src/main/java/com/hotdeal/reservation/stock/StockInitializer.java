package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final StockRedisRepository stockRedisRepository;

    @Override
    public void run(ApplicationArguments args) {
        productRepository.findAll().forEach(product ->
                stockRedisRepository.set(product.getId(), product.getStock().getQuantity())
        );
    }
}