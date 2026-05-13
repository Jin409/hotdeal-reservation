package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.ServiceTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class BookingConcurrencyTest extends ServiceTest {

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
    void 동시에_100명이_예약해도_10명만_성공한다() throws InterruptedException {
        int stock = 10;
        int threadCount = 100;
        Product product = productRepository.save(
                new Product("제주 호텔", 100000, stock,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0)));
        stockRedisRepository.set(product.getId(), stock);

        List<Long> userIds = new ArrayList<>();
        List<Long> bookingIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User user = userRepository.save(new User("유저" + i, "user" + i + "@test.com", 100000L));
            Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
            userIds.add(user.getId());
            bookingIds.add(booking.getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    BookingRequest request = new BookingRequest(product.getId(), List.of(
                            new PaymentMethodRequest("CREDIT_CARD", 100000)
                    ));
                    bookingService.book(userIds.get(index), bookingIds.get(index), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long confirmedCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(10),
                () -> assertThat(confirmedCount).isEqualTo(10),
                () -> assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("0")
        );
    }
}