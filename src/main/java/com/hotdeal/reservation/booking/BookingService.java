package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.product.StockService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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

        pay(bookingId, userId, user, request, product);
        complete(bookingId, userId, request, product);
        queueService.leave(product.getId(), user.getId());

        return new BookingResponse(bookingId, BookingStatus.CONFIRMED);
    }

    private void qualifyToBook(Long userId, Booking booking, Product product) {
        booking.validateNotCompleted();
        queueService.validateIsReady(product.getId(), userId);
    }

    private void pay(Long bookingId, Long userId, User user, BookingRequest request, Product product) {
        try {
            paymentService.validate(user, request.paymentMethods(), product.getPrice());
            paymentService.processExternalPayments(bookingId, user, request.paymentMethods());
        } catch (Exception e) {
            stockService.rollback(product.getId());
            bookingTransactionService.cancel(bookingId);
            throw e;
        }
    }

    private void complete(Long bookingId, Long userId, BookingRequest request, Product product) {
        try {
            bookingTransactionService.complete(bookingId, userId, request.paymentMethods(), product.getPrice());
        } catch (Exception e) {
            cancelPaymentSafely(bookingId, request);
            stockService.rollback(product.getId());
            bookingTransactionService.cancel(bookingId);
            throw e;
        }
    }

    private void cancelPaymentSafely(Long bookingId, BookingRequest request) {
        try {
            paymentService.cancelExternalPayments(bookingId, request.paymentMethods());
        } catch (Exception e) {
            log.warn("결제 취소에 실패했습니다. 확인이 필요합니다. bookingId={}", bookingId, e);
        }
    }
}