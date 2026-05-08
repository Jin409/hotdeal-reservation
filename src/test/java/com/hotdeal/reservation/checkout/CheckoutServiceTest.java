package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.NotFoundException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class CheckoutServiceTest extends ServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 상품_정보와_포인트_잔액을_조회한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));

        CheckoutResponse response = checkoutService.checkout(product.getId(), user.getId());

        assertAll(
                () -> assertThat(response.productName()).isEqualTo("제주 호텔"),
                () -> assertThat(response.price()).isEqualTo(100000),
                () -> assertThat(response.pointBalance()).isEqualTo(50000L)
        );
    }

    @Test
    void 존재하지_않는_상품이면_예외가_발생한다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));

        assertThatThrownBy(() -> checkoutService.checkout(999L, user.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 존재하지_않는_사용자이면_예외가_발생한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );

        assertThatThrownBy(() -> checkoutService.checkout(product.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }
}