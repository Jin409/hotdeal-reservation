package com.hotdeal.reservation.product;

import com.hotdeal.reservation.common.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Column(name = "stock", nullable = false)
    private int quantity;

    public Stock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public boolean isEmpty() {
        return this.quantity <= 0;
    }

    public Stock decrease() {
        if (this.quantity <= 0) {
            throw new BadRequestException("재고가 없습니다.");
        }
        return new Stock(this.quantity - 1);
    }
}