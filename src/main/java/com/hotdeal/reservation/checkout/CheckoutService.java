package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.common.idempotency.IdempotencyStore;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final EntityUtils entityUtils;
    private final QueueService queueService;
    private final BookingRepository bookingRepository;
    private final IdempotencyStore idempotencyStore;

    @Transactional
    public CheckoutResponse checkout(Long productId, Long userId) {
        Product product = entityUtils.getEntity(productId, Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        product.validateInStock();

        Long rank = queueService.enter(productId, userId);
        String idempotencyKey = idempotencyStore.issue(productId + ":" + userId);
        Booking booking = bookingRepository.save(Booking.waiting(userId, productId));

        return CheckoutResponse.of(product, user, rank, booking.getId(), idempotencyKey);
    }
}