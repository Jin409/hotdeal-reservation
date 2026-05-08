package com.hotdeal.reservation.user;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PointTest {

    @Test
    void 포인트를_사용하면_잔액이_차감된다() {
        Point point = new Point(50000);

        Point used = point.use(20000);

        assertThat(used.getBalance()).isEqualTo(30000);
    }

    @Test
    void 잔액보다_많은_포인트를_사용하면_예외가_발생한다() {
        Point point = new Point(10000);

        assertThatThrownBy(() -> point.use(20000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 음수로_포인트를_생성하면_예외가_발생한다() {
        assertThatThrownBy(() -> new Point(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 잔액이_충분한지_확인한다() {
        Point point = new Point(50000);

        assertAll(
                () -> assertThat(point.hasEnough(50000)).isTrue(),
                () -> assertThat(point.hasEnough(50001)).isFalse()
        );
    }
}