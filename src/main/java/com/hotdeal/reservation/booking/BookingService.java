package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.payment.PaymentType;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final EntityUtils entityUtils;
    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse book(Long userId, BookingRequest request) {
        Product product = entityUtils.getEntity(request.getProductId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        validatePaymentRequest(request.getPaymentMethods(), product.getPrice(), user.getPointBalance());
        usePoints(user, request.getPaymentMethods());

        Booking booking = bookingRepository.save(new Booking(userId, product.getId()));

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void validatePaymentRequest(List<PaymentMethodRequest> paymentMethods, int productPrice,
                                        Long pointBalance) {
        validatePaymentCombination(paymentMethods);
        validateTotalAmountMatchesPrice(paymentMethods, productPrice);
        validateHasEnoughPoint(paymentMethods, pointBalance);
    }

    private void validatePaymentCombination(List<PaymentMethodRequest> paymentMethods) {
        Set<PaymentType> types = paymentMethods.stream().map(pm -> PaymentType.valueOf(pm.getType()))
                .collect(Collectors.toSet());

        if (types.contains(PaymentType.CREDIT_CARD) && types.contains(PaymentType.YPAY)) {
            throw new IllegalArgumentException("신용카드와 Y페이는 동시에 사용할 수 없습니다.");
        }
    }

    private void validateTotalAmountMatchesPrice(List<PaymentMethodRequest> paymentMethods, int productPrice) {
        int totalAmount = paymentMethods.stream().mapToInt(PaymentMethodRequest::getAmount).sum();

        if (totalAmount != productPrice) {
            throw new IllegalArgumentException("총 결제금액이 상품 가격과 일치하지 않습니다.");
        }
    }

    private void validateHasEnoughPoint(List<PaymentMethodRequest> paymentMethods, Long pointBalance) {
        int pointAmount = calculatePointToUse(paymentMethods);
        if (pointAmount > pointBalance) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다.");
        }
    }

    private void usePoints(User user, List<PaymentMethodRequest> paymentMethods) {
        int pointAmount = calculatePointToUse(paymentMethods);
        if (pointAmount > 0) {
            user.usePoints(pointAmount);
        }
    }

    private int calculatePointToUse(List<PaymentMethodRequest> paymentMethods) {
        return paymentMethods.stream().filter(pm -> PaymentType.valueOf(pm.getType()) == PaymentType.YPOINT)
                .mapToInt(PaymentMethodRequest::getAmount).sum();
    }
}