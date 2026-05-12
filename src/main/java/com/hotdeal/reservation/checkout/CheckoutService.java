package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.idempotency.IdempotencyStore;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.DuplicateEntryException;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

        if (product.getStock().isEmpty()) {
            throw new BadRequestException("재고가 없습니다.");
        }

        Long rank = enterQueue(productId, userId);
        String idempotencyKey = issueIdempotencyKey(productId, userId);
        Booking booking = bookingRepository.save(Booking.waiting(userId, productId));

        return CheckoutResponse.of(product, user, rank, booking.getId(), idempotencyKey);
    }

    private Long enterQueue(Long productId, Long userId) {
        try {
            return queueService.enter(productId, userId);
        } catch (DuplicateEntryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 장애로 대기열 진입을 건너뜁니다.", e);
            return null;
        }
    }

    private String issueIdempotencyKey(Long productId, Long userId) {
        try {
            return idempotencyStore.issue(productId + ":" + userId);
        } catch (Exception e) {
            log.warn("Redis 장애로 멱등키 발급을 건너뜁니다.", e);
            return null;
        }
    }
}