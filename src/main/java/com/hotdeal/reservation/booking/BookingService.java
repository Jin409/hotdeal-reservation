package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentService;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final long FIRST_IN_LINE = 1;

    private final EntityUtils entityUtils;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final QueueRedisRepository queueRedisRepository;
    private final QueueService queueService;
    private final BookingCommandService bookingCommandService;

    @Transactional
    public BookingResponse book(Long userId, Long bookingId, BookingRequest request) {
        Booking booking = entityUtils.getEntity(bookingId, Booking.class);
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        validateIsFirstInLine(product.getId(), userId);
        stockService.decrease(product.getId());

        try {
            paymentService.pay(bookingId, user, request.paymentMethods(), product.getPrice());
            product.decreaseStock();
            booking.confirm();
            queueService.leave(product.getId(), userId);
        } catch (Exception e) {
            stockService.rollback(product.getId());
            bookingCommandService.cancel(bookingId);
            throw e;
        }

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void validateIsFirstInLine(Long productId, Long userId) {
        Long rank = queueRedisRepository.getRank(productId, userId);
        if (rank == null || rank != FIRST_IN_LINE) {
            throw new BadRequestException("아직 결제할 수 있는 순번이 아닙니다.");
        }
    }
}