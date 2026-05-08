package com.hotdeal.reservation.booking.dto;

import com.hotdeal.reservation.booking.BookingStatus;
import lombok.Getter;

@Getter
public class BookingResponse {

    private final Long bookingId;
    private final BookingStatus status;

    public BookingResponse(Long bookingId, BookingStatus status) {
        this.bookingId = bookingId;
        this.status = status;
    }
}