package com.hotdeal.reservation.product;

import com.hotdeal.reservation.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;

    @Transactional
    public void decrease(Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BadRequestException("상품을 찾을 수 없습니다."));
        product.decreaseStock();
    }

    @Transactional
    public void rollback(Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BadRequestException("상품을 찾을 수 없습니다."));
        product.increaseStock();
    }
}