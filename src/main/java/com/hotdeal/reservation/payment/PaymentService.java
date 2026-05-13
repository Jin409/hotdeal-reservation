package com.hotdeal.reservation.payment;

import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.payment.processor.PaymentProcessor;
import com.hotdeal.reservation.payment.processor.PaymentProcessorFactory;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorFactory processorFactory;
    private final PaymentRepository paymentRepository;
    private final PaymentItemRepository paymentItemRepository;

    public void validate(User user, List<PaymentMethodRequest> methods, long productPrice) {
        PaymentMethods paymentMethods = new PaymentMethods(methods);
        paymentMethods.validateTotalAmount(productPrice);
        user.validatePointBalance(paymentMethods.pointAmount());
    }

    public void processExternalPayments(Long bookingId, User user, List<PaymentMethodRequest> methods) {
        PaymentMethods paymentMethods = new PaymentMethods(methods);
        String pgIdempotencyKey = generatePgIdempotencyKey(bookingId);

        paymentMethods.externalMethods().forEach(pm -> {
            processByPaymentMethod(user, pm, pgIdempotencyKey);
        });
    }

    public void savePaymentResult(Long bookingId, User user, List<PaymentMethodRequest> methods,
                                  long productPrice) {
        PaymentMethods paymentMethods = new PaymentMethods(methods);
        Payment payment = paymentRepository.save(new Payment(bookingId, productPrice));
        savePaymentItems(payment.getId(), methods);

        user.usePoints(paymentMethods.pointAmount());
        payment.succeed();
    }

    public void cancelExternalPayments(Long bookingId, List<PaymentMethodRequest> methods) {
        PaymentMethods paymentMethods = new PaymentMethods(methods);
        String pgIdempotencyKey = generatePgIdempotencyKey(bookingId);

        paymentMethods.externalMethods().forEach(pm -> {
            PaymentType type = PaymentType.valueOf(pm.type());
            PaymentProcessor processor = processorFactory.getProcessor(type);
            processor.cancel(pgIdempotencyKey);
        });
    }

    private void processByPaymentMethod(User user, PaymentMethodRequest pm, String pgIdempotencyKey) {
        PaymentType type = PaymentType.valueOf(pm.type());
        PaymentProcessor processor = processorFactory.getProcessor(type);
        processor.process(pgIdempotencyKey, user, pm.amount());
    }

    private void savePaymentItems(Long paymentId, List<PaymentMethodRequest> methods) {
        methods.forEach(pm -> {
            PaymentType type = PaymentType.valueOf(pm.type());
            paymentItemRepository.save(new PaymentItem(paymentId, type, pm.amount()));
        });
    }

    private String generatePgIdempotencyKey(Long bookingId) {
        return "pg:" + bookingId + ":" + UUID.randomUUID();
    }
}