package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings/{bookingId}")
    public ResponseEntity<BookingResponse> book(
            @RequestHeader("userId") Long userId,
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.book(userId, bookingId, request);
        return ResponseEntity.ok(response);
    }
}