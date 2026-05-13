package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PaymentMethodsTest {

    @Test
    void 신용카드와_Y페이를_동시에_사용하면_예외가_발생한다() {
        assertThatThrownBy(() -> new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPAY", 50000)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("신용카드와 Y페이");
    }

    @Test
    void 신용카드와_포인트_조합은_허용된다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000),
                new PaymentMethodRequest("YPOINT", 20000)
        ));

        assertAll(
                () -> assertThat(methods.externalMethods()).hasSize(1),
                () -> assertThat(methods.pointAmount()).isEqualTo(20000)
        );
    }

    @Test
    void 총_결제금액이_상품_가격과_다르면_예외가_발생한다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000)
        ));

        assertThatThrownBy(() -> methods.validateTotalAmount(100000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 가격과 일치하지 않습니다");
    }

    @Test
    void 총_결제금액이_상품_가격과_일치하면_검증을_통과한다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000),
                new PaymentMethodRequest("YPOINT", 20000)
        ));

        methods.validateTotalAmount(100000);
    }

    @Test
    void 포인트_금액을_계산한다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000),
                new PaymentMethodRequest("YPOINT", 20000)
        ));

        assertThat(methods.pointAmount()).isEqualTo(20000);
    }

    @Test
    void 포인트가_없으면_0을_반환한다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThat(methods.pointAmount()).isEqualTo(0);
    }

    @Test
    void 외부_결제_수단만_필터링한다() {
        PaymentMethods methods = new PaymentMethods(List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000),
                new PaymentMethodRequest("YPOINT", 20000)
        ));

        assertAll(
                () -> assertThat(methods.externalMethods()).hasSize(1),
                () -> assertThat(methods.externalMethods().get(0).type()).isEqualTo("CREDIT_CARD")
        );
    }
}