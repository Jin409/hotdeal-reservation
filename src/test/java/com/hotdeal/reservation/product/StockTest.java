package com.hotdeal.reservation.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void 재고를_차감하면_수량이_1_감소한다() {
        Stock stock = new Stock(10);

        Stock decreased = stock.decrease();

        assertThat(decreased.getQuantity()).isEqualTo(9);
    }

    @Test
    void 재고가_0일_때_차감하면_예외가_발생한다() {
        Stock stock = new Stock(0);

        assertThatThrownBy(stock::decrease)
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void 음수로_재고를_생성하면_예외가_발생한다() {
        assertThatThrownBy(() -> new Stock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}