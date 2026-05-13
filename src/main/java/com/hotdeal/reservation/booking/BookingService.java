package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final EntityUtils entityUtils;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final QueueService queueService;
    private final BookingTransactionService bookingTransactionService;

    public BookingResponse book(Long userId, Long bookingId, BookingRequest request) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        qualifyToBook(userId, booking, product);
        stockService.decrease(product.getId());

        try {
            paymentService.validate(user, request.paymentMethods(), product.getPrice());
            paymentService.processExternalPayments(bookingId, user, request.paymentMethods());
            bookingTransactionService.complete(bookingId, userId, request.paymentMethods(), product.getPrice());
        } catch (Exception e) {
            stockService.rollback(product.getId());
            bookingTransactionService.cancel(bookingId);
            throw e;
        }

        queueService.leave(product.getId(), user.getId());

        return new BookingResponse(bookingId, BookingStatus.CONFIRMED);
    }

    private void qualifyToBook(Long userId, Booking booking, Product product) {
        booking.validateNotCompleted();
        queueService.validateIsReady(product.getId(), userId);
    }
}