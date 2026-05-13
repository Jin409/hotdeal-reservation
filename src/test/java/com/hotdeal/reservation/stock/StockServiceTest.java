package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockServiceTest extends ServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void 재고를_차감하면_1_감소한다() {
        Product product = productRepository.save(
                new Product("테스트 호텔", 100000, 10,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1)));

        stockService.decrease(product.getId());

        Product updated = productRepository.findById(product.getId()).get();
        assertThat(updated.getStock().getQuantity()).isEqualTo(9);
    }

    @Test
    void 재고가_0일_때_차감하면_예외가_발생한다() {
        Product product = productRepository.save(
                new Product("테스트 호텔", 100000, 0,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> stockService.decrease(product.getId()))
                .isInstanceOf(BadRequestException.class);
    }
}