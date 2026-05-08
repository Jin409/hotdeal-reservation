package com.hotdeal.reservation.booking.dto;

import com.hotdeal.reservation.booking.BookingStatus;

public record BookingResponse(
        Long bookingId,
        BookingStatus status
) {
}