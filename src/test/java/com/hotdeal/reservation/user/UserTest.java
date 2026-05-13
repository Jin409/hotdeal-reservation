package com.hotdeal.reservation.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void 포인트_잔액이_충분하면_검증을_통과한다() {
        User user = new User("홍길동", "hong@test.com", 50000L);

        user.validatePointBalance(50000L);
    }

    @Test
    void 포인트_잔액이_부족하면_예외가_발생한다() {
        User user = new User("홍길동", "hong@test.com", 10000L);

        assertThatThrownBy(() -> user.validatePointBalance(50000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("포인트 잔액이 부족합니다");
    }

    @Test
    void 포인트를_사용하면_잔액이_차감된다() {
        User user = new User("홍길동", "hong@test.com", 50000L);

        user.usePoints(20000L);

        assertThat(user.getPointBalance()).isEqualTo(30000L);
    }
}