package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.product.OutOfStockException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 예약_성공시_재고가_1_감소하고_CONFIRMED_상태가_된다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        BookingResponse response = bookingService.book(user.getId(), request);

        Product updated = productRepository.findById(product.getId()).get();
        assertAll(
                () -> assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED),
                () -> assertThat(updated.getStock().getQuantity()).isEqualTo(9)
        );
    }

    @Test
    void 재고가_없으면_예외가_발생한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 0,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        User user = userRepository.save(new User("홍길동", "hong@test.com", 100000L));
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), request))
                .isInstanceOf(OutOfStockException.class);
    }
}