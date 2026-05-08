package com.hotdeal.reservation.checkout;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestParam Long productId,
            @RequestHeader("userId") Long userId) {
        CheckoutResponse response = checkoutService.checkout(productId, userId);
        return ResponseEntity.ok(response);
    }
}