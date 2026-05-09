package com.hotdeal.reservation.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByProductIdAndUserId(Long productId, Long userId);
}