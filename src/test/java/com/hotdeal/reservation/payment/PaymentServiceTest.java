package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void 신용카드와_Y페이_혼용시_예외가_발생한다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        List<PaymentMethodRequest> methods = List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPAY", 50000)
        );

        assertThatThrownBy(() -> paymentService.pay(user, methods, 100000))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("신용카드와 Y페이");
    }

    @Test
    void 총_결제금액이_상품_가격과_다르면_예외가_발생한다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        List<PaymentMethodRequest> methods = List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000)
        );

        assertThatThrownBy(() -> paymentService.pay(user, methods, 100000))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("상품 가격과 일치하지 않습니다");
    }

    @Test
    void 포인트_잔액이_부족하면_예외가_발생한다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 10000L));
        List<PaymentMethodRequest> methods = List.of(
                new PaymentMethodRequest("YPOINT", 50000)
        );

        assertThatThrownBy(() -> paymentService.pay(user, methods, 50000))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("포인트 잔액이 부족합니다");
    }

    @Test
    void 카드와_포인트_복합결제_성공시_포인트가_차감된다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        List<PaymentMethodRequest> methods = List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000),
                new PaymentMethodRequest("YPOINT", 20000)
        );

        paymentService.pay(user, methods, 100000);

        assertThat(user.getPointBalance()).isEqualTo(30000);
    }

    @Test
    void 카드_단독_결제시_포인트가_차감되지_않는다() {
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        List<PaymentMethodRequest> methods = List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        );

        paymentService.pay(user, methods, 100000);

        assertThat(user.getPointBalance()).isEqualTo(50000);
    }
}