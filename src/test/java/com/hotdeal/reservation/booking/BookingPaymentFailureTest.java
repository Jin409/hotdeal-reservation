package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.payment.PaymentMethodRequest;
import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentRepository;
import com.hotdeal.reservation.payment.client.CardPgClient;
import com.hotdeal.reservation.common.exception.PgErrorCode;
import com.hotdeal.reservation.common.exception.PgException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doThrow;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class BookingPaymentFailureTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private CardPgClient cardPgClient;

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void 카드_거절시_CANCELLED_상태로_변경되고_재고가_롤백된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new PgException(PgErrorCode.INVALID_REJECT_CARD))
                .when(cardPgClient).charge(anyString(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(BadRequestException.class);

        Booking updatedBooking = bookingRepository.findById(booking.getId()).get();
        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertAll(
                () -> assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED),
                () -> assertThat(updatedProduct.getStock().getQuantity()).isEqualTo(10)
        );
    }

    @Test
    void PG사_일시장애시_재시도_후_최종_실패하면_CANCELLED_상태로_변경된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new PgException(PgErrorCode.PROVIDER_ERROR))
                .when(cardPgClient).charge(anyString(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(PgException.class);

        Booking updatedBooking = bookingRepository.findById(booking.getId()).get();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void PG사_일시장애_후_재시도에서_성공하면_CONFIRMED가_된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new PgException(PgErrorCode.PROVIDER_ERROR))
                .doNothing()
                .when(cardPgClient).charge(anyString(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        var response = bookingService.book(user.getId(), booking.getId(), request);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
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