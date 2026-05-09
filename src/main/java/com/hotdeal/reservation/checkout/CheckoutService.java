package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.product.OutOfStockException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final EntityUtils entityUtils;
    private final QueueService queueService;

    public CheckoutResponse checkout(Long productId, Long userId) {
        Product product = entityUtils.getEntity(productId, Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        if (product.getStock().isEmpty()) {
            throw new OutOfStockException("재고가 없습니다.");
        }

        Long rank = queueService.enter(productId, userId);

        return CheckoutResponse.of(product, user, rank);
    }
}