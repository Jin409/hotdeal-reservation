package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CheckoutResponse checkout(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new CheckoutResponse(
                product.getName(),
                product.getPrice(),
                product.getCheckInAt(),
                product.getCheckOutAt(),
                user.getPointBalance()
        );
    }
}