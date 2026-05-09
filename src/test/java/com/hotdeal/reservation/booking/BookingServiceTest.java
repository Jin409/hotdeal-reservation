package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.stock.StockKeys;
import com.hotdeal.reservation.stock.StockRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

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

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void 예약_성공시_CONFIRMED_상태가_된다() {
        Product product = createProduct(10);
        User user = createUser(50000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        BookingResponse response = bookingService.book(user.getId(), booking.getId(), request);

        User updatedUser = userRepository.findById(user.getId()).get();
        assertAll(
                () -> assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED),
                () -> assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("9"),
                () -> assertThat(updatedUser.getPointBalance()).isEqualTo(0L)
        );
    }

    @Test
    void 재고가_없으면_예외가_발생한다() {
        Product product = createProduct(0);
        User user = createUser(100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 0);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 결제_실패시_Redis_재고가_롤백된다() {
        Product product = createProduct(10);
        User user = createUser(10000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        try {
            bookingService.book(user.getId(), booking.getId(), request);
        } catch (BadRequestException ignored) {
        }

        String remaining = redisTemplate.opsForValue().get(StockKeys.stock(product.getId()));
        assertThat(remaining).isEqualTo("10");
    }

    @Test
    void 동시에_여러명이_요청해도_수량만큼만_예약에_성공한다() throws InterruptedException {
        Product product = createProduct(10);
        stockRedisRepository.set(product.getId(), 10);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User user = userRepository.save(new User("유저" + index, "user" + index + "@test.com", 100000L));
                    Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

                    BookingRequest request = new BookingRequest(product.getId(), List.of(
                            new PaymentMethodRequest("CREDIT_CARD", 100000)
                    ));

                    bookingService.book(user.getId(), booking.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("0")
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