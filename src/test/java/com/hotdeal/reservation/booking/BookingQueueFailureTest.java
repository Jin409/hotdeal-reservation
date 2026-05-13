package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EmbeddedRedisConfig;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class BookingQueueFailureTest {

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
    void 대기열_제거_실패해도_결제는_CONFIRMED가_유지된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        given(queueRedisRepository.isReady(anyLong(), anyLong())).willReturn(true);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(queueRedisRepository).popFirst(anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        var response = bookingService.book(user.getId(), booking.getId(), request);

        Booking updated = bookingRepository.findById(booking.getId()).get();
        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertAll(
                () -> assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED),
                () -> assertThat(updated.getStatus()).isEqualTo(BookingStatus.CONFIRMED),
                () -> assertThat(updatedProduct.getStock().getQuantity()).isEqualTo(9)
        );
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