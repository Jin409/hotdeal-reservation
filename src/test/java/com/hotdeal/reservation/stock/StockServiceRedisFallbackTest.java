package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class StockServiceRedisFallbackTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private StockRedisRepository stockRedisRepository;

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void Redis_장애시_DB_비관적_락으로_재고를_차감한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        given(stockRedisRepository.decrease(anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        stockService.decrease(product.getId());

        Product updated = productRepository.findById(product.getId()).get();
        assertThat(updated.getStock().getQuantity()).isEqualTo(9);
    }

    @Test
    void Redis_장애시_DB_재고가_0이면_예외가_발생한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 0,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        given(stockRedisRepository.decrease(anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        assertThatThrownBy(() -> stockService.decrease(product.getId()))
                .isInstanceOf(BadRequestException.class);
    }
}