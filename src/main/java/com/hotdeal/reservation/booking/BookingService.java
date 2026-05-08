package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EntityUtils;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentService;
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
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse book(Long userId, BookingRequest request) {
        Product product = entityUtils.getEntity(request.productId(), Product.class);
        User user = entityUtils.getEntity(userId, User.class);

        validatePaymentRequest(request.paymentMethods(), product.getPrice(), user);
        paymentService.pay(user, request.paymentMethods());

        Booking booking = bookingRepository.save(new Booking(userId, product.getId()));

        return new BookingResponse(booking.getId(), booking.getStatus());
    }

    private void validatePaymentRequest(List<PaymentMethodRequest> paymentMethods, int productPrice, User user) {
        validatePaymentCombination(paymentMethods);
        validateTotalAmountMatchesPrice(paymentMethods, productPrice);
        validateHasEnoughPoint(paymentMethods, user);
    }

    private void validatePaymentCombination(List<PaymentMethodRequest> paymentMethods) {
        Set<PaymentType> types = paymentMethods.stream().map(pm -> PaymentType.valueOf(pm.type()))
                .collect(Collectors.toSet());

        if (types.contains(PaymentType.CREDIT_CARD) && types.contains(PaymentType.YPAY)) {
            throw new BadRequestException("신용카드와 Y페이는 동시에 사용할 수 없습니다.");
        }
    }

    private void validateTotalAmountMatchesPrice(List<PaymentMethodRequest> paymentMethods, int productPrice) {
        int totalAmount = paymentMethods.stream().mapToInt(PaymentMethodRequest::amount).sum();

        if (totalAmount != productPrice) {
            throw new BadRequestException("총 결제금액이 상품 가격과 일치하지 않습니다.");
        }
    }

    private void validateHasEnoughPoint(List<PaymentMethodRequest> paymentMethods, User user) {
        int pointAmount = calculatePointToUse(paymentMethods);
        if (!user.getPoint().hasEnough(pointAmount)) {
            throw new BadRequestException("포인트 잔액이 부족합니다.");
        }
    }

    private int calculatePointToUse(List<PaymentMethodRequest> paymentMethods) {
        return paymentMethods.stream().filter(pm -> PaymentType.valueOf(pm.type()) == PaymentType.YPOINT)
                .mapToInt(PaymentMethodRequest::amount).sum();
    }
}