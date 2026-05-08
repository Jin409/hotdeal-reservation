package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {

    private final EntityUtils entityUtils;

    public CheckoutResponse checkout(Long productId, Long userId) {
        Product product = entityUtils.getEntity(productId, Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        return new CheckoutResponse(
                product.getName(),
                product.getPrice(),
                product.getCheckInAt(),
                product.getCheckOutAt(),
                user.getPointBalance()
        );
    }
}