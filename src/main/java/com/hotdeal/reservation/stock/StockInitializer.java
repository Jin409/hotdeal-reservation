package com.hotdeal.reservation.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInitializer implements ApplicationRunner {

    private final StockSynchronizer stockSynchronizer;

    @Override
    public void run(ApplicationArguments args) {
        try {
            stockSynchronizer.syncAll();
        } catch (Exception e) {
            log.warn("Redis 재고 초기화 실패. Redis 복구 후 자동 동기화됩니다.", e);
        }
    }
}