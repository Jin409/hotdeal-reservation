package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingTransactionService {

    private final EntityUtils entityUtils;
    private final PaymentService paymentService;

    @Transactional
    public void complete(Long bookingId, Long userId,
                         List<PaymentMethodRequest> paymentMethods, long price) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        User user = entityUtils.getEntity(userId, User.class);
        paymentService.savePaymentResult(bookingId, user, paymentMethods, price);
        booking.confirm();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(Long bookingId) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        booking.cancel();
    }
}