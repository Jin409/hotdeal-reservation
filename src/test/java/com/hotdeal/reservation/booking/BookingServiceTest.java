package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.BookingResponse;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.payment.PaymentItemRepository;
import com.hotdeal.reservation.payment.PaymentRepository;
import com.hotdeal.reservation.payment.PaymentStatus;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueKeys;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockKeys;
import com.hotdeal.reservation.stock.StockRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

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
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    @Autowired
    private QueueRedisRepository queueRedisRepository;

    @Test
    void 예약_성공시_결제_처리되고_대기열에서_제거된다() {
        Product product = createProduct(10);
        User firstUser = createUser("첫번째", 50000L);
        User secondUser = createUser("두번째", 100000L);
        Booking booking = bookingRepository.save(Booking.waiting(firstUser.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);
        queueService.enter(product.getId(), firstUser.getId());
        queueService.enter(product.getId(), secondUser.getId());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        BookingResponse response = bookingService.book(firstUser.getId(), booking.getId(), request);

        User updatedUser = userRepository.findById(firstUser.getId()).get();
        assertAll(
                () -> assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED),
                () -> assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("9"),
                () -> assertThat(updatedUser.getPointBalance()).isEqualTo(0L),
                () -> assertThat(paymentRepository.findAll()).hasSize(1),
                () -> assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS),
                () -> assertThat(paymentItemRepository.findAll()).hasSize(2),
                () -> assertThat(queueRedisRepository.getRank(product.getId(), firstUser.getId())).isNull(),
                () -> assertThat(queueRedisRepository.getRank(product.getId(), secondUser.getId())).isEqualTo(1L)
        );
    }

    @Test
    void 재고가_없으면_예외가_발생한다() {
        Product product = createProduct(0);
        User user = createUser("홍길동", 100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 0);
        queueService.enter(product.getId(), user.getId());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 순번이_아닌_사용자가_결제하면_예외가_발생한다() {
        Product product = createProduct(10);
        User firstUser = createUser("첫번째", 100000L);
        User secondUser = createUser("두번째", 100000L);
        Booking booking = bookingRepository.save(Booking.waiting(secondUser.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);
        queueService.enter(product.getId(), firstUser.getId());
        queueService.enter(product.getId(), secondUser.getId());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        assertThatThrownBy(() -> bookingService.book(secondUser.getId(), booking.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("순번");
    }

    @Test
    void 결제_실패시_재고가_롤백되고_Booking이_CANCELLED가_된다() {
        Product product = createProduct(10);
        User user = createUser("홍길동", 10000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);
        queueService.enter(product.getId(), user.getId());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);

        Booking updated = bookingRepository.findById(booking.getId()).get();
        assertAll(
                () -> assertThat(redisTemplate.opsForValue().get(StockKeys.stock(product.getId()))).isEqualTo("10"),
                () -> assertThat(updated.getStatus()).isEqualTo(BookingStatus.CANCELLED)
        );
    }

    @Test
    void 이미_완료된_예약에_다시_결제하면_예외가_발생한다() {
        Product product = createProduct(10);
        User user = createUser("홍길동", 100000L);
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);
        queueService.enter(product.getId(), user.getId());

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        bookingService.book(user.getId(), booking.getId(), request);

        queueService.enter(product.getId(), user.getId());

        assertThatThrownBy(() -> bookingService.book(user.getId(), booking.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료된 예약");
    }

    private Product createProduct(int stock) {
        return productRepository.save(
                new Product("제주 호텔", 100000, stock,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
    }

    private User createUser(String name, long pointBalance) {
        return userRepository.save(new User(name, name + "@test.com", pointBalance));
    }
}