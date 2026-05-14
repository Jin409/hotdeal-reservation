package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.payment.PaymentMethodRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PaymentMethods {

    private final List<PaymentMethodRequest> methods;

    public PaymentMethods(List<PaymentMethodRequest> methods) {
        this.methods = methods;
        validateCombination();
    }

    public void validateTotalAmount(long expectedPrice) {
        long totalAmount = methods.stream().mapToLong(PaymentMethodRequest::amount).sum();
        if (totalAmount != expectedPrice) {
            throw new IllegalArgumentException("총 결제금액이 상품 가격과 일치하지 않습니다.");
        }
    }

    public long pointAmount() {
        return methods.stream()
                .filter(this::isPoint)
                .mapToLong(PaymentMethodRequest::amount)
                .sum();
    }

    public List<PaymentMethodRequest> all() {
        return methods;
    }

    public List<PaymentMethodRequest> externalMethods() {
        return methods.stream()
                .filter(pm -> !isPoint(pm))
                .toList();
    }

    private void validateCombination() {
        Set<PaymentType> types = methods.stream()
                .map(pm -> PaymentType.valueOf(pm.type()))
                .collect(Collectors.toSet());

        if (types.contains(PaymentType.CREDIT_CARD) && types.contains(PaymentType.YPAY)) {
            throw new IllegalArgumentException("신용카드와 Y페이는 동시에 사용할 수 없습니다.");
        }
    }

    private boolean isPoint(PaymentMethodRequest pm) {
        return PaymentType.valueOf(pm.type()) == PaymentType.YPOINT;
    }
}