package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRedisRepository stockRedisRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void decrease(Long productId) {
        try {
            decreaseByRedis(productId);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 재고 차감 실패. DB 비관적 락으로 전환합니다.", e);
            decreaseByDb(productId);
        }
    }

    public void rollback(Long productId) {
        try {
            stockRedisRepository.increase(productId);
        } catch (Exception e) {
            log.warn("Redis 재고 롤백 실패.", e);
        }
    }

    private void decreaseByRedis(Long productId) {
        Long remaining = stockRedisRepository.decrease(productId);
        if (remaining < 0) {
            stockRedisRepository.increase(productId);
            throw new BadRequestException("재고가 없습니다.");
        }
    }

    private void decreaseByDb(Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BadRequestException("상품을 찾을 수 없습니다."));
        product.decreaseStock();
    }
}