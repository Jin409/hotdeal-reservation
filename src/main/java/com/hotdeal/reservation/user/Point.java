package com.hotdeal.reservation.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    @Column(name = "point_balance", nullable = false)
    private long balance;

    public Point(long balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
        this.balance = balance;
    }

    public boolean hasEnough(long amount) {
        return this.balance >= amount;
    }

    public Point use(long amount) {
        if (!hasEnough(amount)) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        return new Point(this.balance - amount);
    }
}