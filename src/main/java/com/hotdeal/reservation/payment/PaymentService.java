package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.payment.processor.PaymentProcessor;
import com.hotdeal.reservation.payment.processor.PaymentProcessorFactory;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorFactory processorFactory;

    public void pay(User user, List<PaymentMethodRequest> paymentMethods) {
        processExternalPayments(user, paymentMethods);
        processPointPayment(user, paymentMethods);
    }

    private void processExternalPayments(User user, List<PaymentMethodRequest> paymentMethods) {
        paymentMethods.stream()
                .filter(pm -> !isPoint(pm))
                .forEach(pm -> execute(user, pm));
    }

    private void processPointPayment(User user, List<PaymentMethodRequest> paymentMethods) {
        paymentMethods.stream()
                .filter(this::isPoint)
                .forEach(pm -> execute(user, pm));
    }

    private void execute(User user, PaymentMethodRequest pm) {
        PaymentType type = PaymentType.valueOf(pm.getType());
        PaymentProcessor processor = processorFactory.getProcessor(type);
        processor.process(user, pm.getAmount());
    }

    private boolean isPoint(PaymentMethodRequest pm) {
        return PaymentType.valueOf(pm.getType()) == PaymentType.YPOINT;
    }
}