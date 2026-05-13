package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class BookingRedisFallbackTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private QueueRedisRepository queueRedisRepository;

    @AfterEach
    void cleanUp() {
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void Redis_장애시_순번_검증을_스킵하고_결제에_성공한다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        given(queueRedisRepository.getRank(anyLong(), anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        var response = bookingService.book(user.getId(), booking.getId(), request);

        Product updated = productRepository.findById(product.getId()).get();
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(updated.getStock().getQuantity()).isEqualTo(9);
    }

    @Test
    void Redis_장애시_이미_완료된_예약에_재요청하면_예외가_발생한다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        given(queueRedisRepository.getRank(anyLong(), anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        bookingService.book(user.getId(), booking.getId(), request);

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료된 예약");
    }

    @Test
    void Redis_장애시_재고가_없으면_예외가_발생한다() {
        Product product = createProduct(0);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        given(queueRedisRepository.getRank(anyLong(), anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    private Product createProduct(int stock) {
        return productRepository.save(
                new Product("제주 호텔", 100000, stock,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
    }

    private User createUser(long pointBalance) {
        return userRepository.save(new User("홍길동", "hong@test.com", pointBalance));
    }
}