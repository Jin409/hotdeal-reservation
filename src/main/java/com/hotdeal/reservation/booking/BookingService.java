package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final EntityUtils entityUtils;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final QueueService queueService;
    private final BookingCommandService bookingCommandService;

    @Transactional
    public BookingResponse book(Long userId, Long bookingId, BookingRequest request) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        validateBookingStatus(booking);
        queueService.validateIsFirstInLine(product.getId(), userId);

        stockService.decrease(product.getId());
        try {
            processPaymentAndConfirm(booking, product, user, bookingId, request);
        } catch (Exception e) {
            handlePaymentFailure(product.getId(), bookingId);
            throw e;
        }

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void processPaymentAndConfirm(Booking booking, Product product, User user,
                                           Long bookingId, BookingRequest request) {
        paymentService.validate(user, request.paymentMethods(), product.getPrice());
        paymentService.processExternalPayments(bookingId, user, request.paymentMethods());
        paymentService.savePaymentResult(bookingId, user, request.paymentMethods(), product.getPrice());
        booking.confirm();
        queueService.leave(product.getId(), user.getId());
    }

    private void handlePaymentFailure(Long productId, Long bookingId) {
        stockService.rollback(productId);
        bookingCommandService.cancel(bookingId);
    }

    private void validateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("이미 완료된 예약입니다.");
        }
    }
}