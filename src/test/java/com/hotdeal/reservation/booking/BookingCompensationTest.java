package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.payment.client.CardPgClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class BookingCompensationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private BookingTransactionService bookingTransactionService;

    @MockitoBean
    private CardPgClient cardPgClient;

    @AfterEach
    void cleanUp() {
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void PG_결제_성공_후_DB_저장_실패시_PG_취소가_호출되고_재고가_원복된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new RuntimeException("DB 저장 실패"))
                .when(bookingTransactionService).complete(anyLong(), anyLong(), any(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(RuntimeException.class);

        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertAll(
                () -> assertThat(updatedProduct.getStock().getQuantity()).isEqualTo(10),
                () -> verify(cardPgClient).cancel(anyString())
        );
    }

    @Test
    void PG_결제와_취소에_동일한_멱등성_키가_전달된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new RuntimeException("DB 저장 실패"))
                .when(bookingTransactionService).complete(anyLong(), anyLong(), any(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<String> chargeKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cancelKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cardPgClient).charge(chargeKeyCaptor.capture(), anyLong());
        verify(cardPgClient).cancel(cancelKeyCaptor.capture());

        assertThat(chargeKeyCaptor.getValue()).isEqualTo(cancelKeyCaptor.getValue());
    }

    @Test
    void PG_결제_실패시에는_PG_취소가_호출되지_않고_재고가_원복된다() {
        Product product = createProduct(10);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        queueService.enter(product.getId(), user.getId());

        doThrow(new RuntimeException("PG 결제 실패"))
                .when(cardPgClient).charge(anyString(), anyLong());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(RuntimeException.class);

        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertAll(
                () -> assertThat(updatedProduct.getStock().getQuantity()).isEqualTo(10),
                () -> verify(cardPgClient, org.mockito.Mockito.never()).cancel(anyString())
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