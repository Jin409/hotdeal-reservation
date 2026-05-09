package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.product.OutOfStockException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.stock.StockRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest extends ServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRedisRepository stockRedisRepository;

    @Test
    void 예약_성공시_CONFIRMED_상태가_된다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        User user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        BookingResponse response = bookingService.book(user.getId(), booking.getId(), request);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void 재고가_없으면_예외가_발생한다() {
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, 0,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        User user = userRepository.save(new User("홍길동", "hong@test.com", 100000L));
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 0);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(OutOfStockException.class);
    }
}
