package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.common.EntityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingCompensationService {

    private final EntityUtils entityUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(Long bookingId) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        booking.cancel();
    }
}