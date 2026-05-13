package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockServiceTest extends ServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRedisRepository stockRedisRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void 재고를_차감하면_1_감소한다() {
        Product product = productRepository.save(
                new Product("테스트 호텔", 100000, 10,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1)));
        stockRedisRepository.set(product.getId(), 10);

        stockService.decrease(product.getId());

        assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("9");
    }

    @Test
    void 재고가_0일_때_차감하면_예외가_발생하고_원복된다() {
        stockRedisRepository.set(1L, 0);

        assertThatThrownBy(() -> stockService.decrease(1L))
                .isInstanceOf(BadRequestException.class);

        assertThat(redisTemplate.opsForValue().get(StockKeys.stock(1L))).isEqualTo("0");
    }

    @Test
    void 롤백하면_재고가_1_증가한다() {
        stockRedisRepository.set(1L, 9);

        stockService.rollback(1L);

        assertThat(redisTemplate.opsForValue().get(StockKeys.stock(1L))).isEqualTo("10");
    }
}