package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EntityUtils;
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
    private final BookingCompensationService bookingCommandService;

    @Transactional
    public BookingResponse book(Long userId, Long bookingId, BookingRequest request) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        qualifyToBook(userId, booking, product);
        book(bookingId, request, product, user, booking);

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void book(Long bookingId, BookingRequest request, Product product, User user, Booking booking) {
        stockService.decrease(product.getId());

        try {
            processPayment(bookingId, user, request.paymentMethods(), product.getPrice());
            booking.confirm();
            queueService.leave(product.getId(), user.getId());
        } catch (Exception e) {
            bookingCommandService.cancel(bookingId);
            throw e;
        }
    }

    private void qualifyToBook(Long userId, Booking booking, Product product) {
        booking.validateNotCompleted();
        queueService.validateIsReady(product.getId(), userId);
    }

    private void processPayment(Long bookingId, User user,
                                List<PaymentMethodRequest> paymentMethods, long price) {
        paymentService.validate(user, paymentMethods, price);
        paymentService.processExternalPayments(bookingId, user, paymentMethods);
        paymentService.savePaymentResult(bookingId, user, paymentMethods, price);
    }
}