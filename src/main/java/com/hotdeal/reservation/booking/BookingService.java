package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
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

import java.util.List;

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

        validateToBook(userId, booking, product);
        order(bookingId, request, product, user, booking);

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void order(Long bookingId, BookingRequest request, Product product, User user, Booking booking) {
        stockService.decrease(product.getId());

        try {
            processPayment(bookingId, user, request.paymentMethods(), product.getPrice());
            booking.confirm();
            queueService.leave(product.getId(), user.getId());
        } catch (Exception e) {
            compensate(product.getId(), bookingId);
            throw e;
        }
    }

    private void validateToBook(Long userId, Booking booking, Product product) {
        validateBookingStatus(booking);
        queueService.validateIsFirstInLine(product.getId(), userId);
    }

    private void processPayment(Long bookingId, User user,
                                List<PaymentMethodRequest> paymentMethods, long price) {
        paymentService.validate(user, paymentMethods, price);
        paymentService.processExternalPayments(bookingId, user, paymentMethods);
        paymentService.savePaymentResult(bookingId, user, paymentMethods, price);
    }

    private void compensate(Long productId, Long bookingId) {
        stockService.rollback(productId);
        bookingCommandService.cancel(bookingId);
    }

    private void validateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("이미 완료된 예약입니다.");
        }
    }
}