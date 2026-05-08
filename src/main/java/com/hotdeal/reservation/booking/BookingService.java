package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final EntityUtils entityUtils;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse book(Long userId, BookingRequest request) {
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        product.decreaseStock();
        paymentService.pay(user, request.paymentMethods(), product.getPrice());

        Booking booking = bookingRepository.save(new Booking(userId, product.getId()));

        return new BookingResponse(booking.getId(), booking.getStatus());
    }
}