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

    public static Booking confirmed(Long userId, Long productId) {
        return new Booking(userId, productId, BookingStatus.CONFIRMED);
    }

    private Booking(Long userId, Long productId, BookingStatus status) {
        this.userId = userId;
        this.productId = productId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
