package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.processor.PaymentProcessor;
import com.hotdeal.reservation.payment.processor.PaymentProcessorFactory;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorFactory processorFactory;

    public void pay(Long bookingId, User user, List<PaymentMethodRequest> paymentMethods, long productPrice) {
        validatePaymentCombination(paymentMethods);
        validateTotalAmountMatchesPrice(paymentMethods, productPrice);
        validateHasEnoughPoint(paymentMethods, user);

        String pgIdempotencyKey = generatePgIdempotencyKey(bookingId);
        processExternalPayments(pgIdempotencyKey, user, paymentMethods);
        processPointPayment(pgIdempotencyKey, user, paymentMethods);
    }

    private String generatePgIdempotencyKey(Long bookingId) {
        return "pg:" + bookingId + ":" + UUID.randomUUID();
    }

    private void validatePaymentCombination(List<PaymentMethodRequest> paymentMethods) {
        Set<PaymentType> types = paymentMethods.stream()
                .map(pm -> PaymentType.valueOf(pm.type()))
                .collect(Collectors.toSet());

        if (types.contains(PaymentType.CREDIT_CARD) && types.contains(PaymentType.YPAY)) {
            throw new BadRequestException("신용카드와 Y페이는 동시에 사용할 수 없습니다.");
        }
    }

    private void validateTotalAmountMatchesPrice(List<PaymentMethodRequest> paymentMethods, long productPrice) {
        long totalAmount = paymentMethods.stream().mapToLong(PaymentMethodRequest::amount).sum();

        if (totalAmount != productPrice) {
            throw new BadRequestException("총 결제금액이 상품 가격과 일치하지 않습니다.");
        }
    }

    private void validateHasEnoughPoint(List<PaymentMethodRequest> paymentMethods, User user) {
        long pointAmount = calculatePointToUse(paymentMethods);
        if (!user.getPoint().hasEnough(pointAmount)) {
            throw new BadRequestException("포인트 잔액이 부족합니다.");
        }
    }

    private void processExternalPayments(String idempotencyKey, User user, List<PaymentMethodRequest> paymentMethods) {
        paymentMethods.stream()
                .filter(pm -> !isPoint(pm))
                .forEach(pm -> execute(idempotencyKey, user, pm));
    }

    private void processPointPayment(String idempotencyKey, User user, List<PaymentMethodRequest> paymentMethods) {
        paymentMethods.stream()
                .filter(this::isPoint)
                .forEach(pm -> execute(idempotencyKey, user, pm));
    }

    private void execute(String idempotencyKey, User user, PaymentMethodRequest pm) {
        PaymentType type = PaymentType.valueOf(pm.type());
        PaymentProcessor processor = processorFactory.getProcessor(type);
        processor.process(idempotencyKey, user, pm.amount());
    }

    private boolean isPoint(PaymentMethodRequest pm) {
        return PaymentType.valueOf(pm.type()) == PaymentType.YPOINT;
    }

    private long calculatePointToUse(List<PaymentMethodRequest> paymentMethods) {
        return paymentMethods.stream()
                .filter(this::isPoint)
                .mapToLong(PaymentMethodRequest::amount)
                .sum();
    }
}