package com.hotdeal.reservation.booking;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Booking waiting(Long userId, Long productId) {
        return new Booking(userId, productId, BookingStatus.WAITING);
    }

    private Booking(Long userId, Long productId, BookingStatus status) {
        this.userId = userId;
        this.productId = productId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void validateNotCompleted() {
        if (this.status == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("이미 완료된 예약입니다.");
        }
    }

    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }
}