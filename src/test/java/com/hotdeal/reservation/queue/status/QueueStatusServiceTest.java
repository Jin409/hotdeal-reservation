package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.BadRequestException;
import com.hotdeal.reservation.common.exception.NotFoundException;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class QueueStatusServiceTest extends ServiceTest {

    @Autowired
    private QueueStatusService queueStatusService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void 대기열_첫번째이면_READY를_반환한다() {
        Product product = createProduct(10);
        User user = createUser("유저1");
        queueService.enter(product.getId(), user.getId());

        QueueStatusResponse response = queueStatusService.getStatus(product.getId(), user.getId());

        assertAll(
                () -> assertThat(response.status()).isEqualTo(QueueStatus.READY),
                () -> assertThat(response.rank()).isEqualTo(1L)
        );
    }

    @Test
    void 대기열_두번째_이후이면_WAITING과_순번을_반환한다() {
        Product product = createProduct(10);
        User user1 = createUser("유저1");
        User user2 = createUser("유저2");
        queueService.enter(product.getId(), user1.getId());
        queueService.enter(product.getId(), user2.getId());

        QueueStatusResponse response = queueStatusService.getStatus(product.getId(), user2.getId());

        assertAll(
                () -> assertThat(response.status()).isEqualTo(QueueStatus.WAITING),
                () -> assertThat(response.rank()).isEqualTo(2L)
        );
    }

    @Test
    void 대기열에_없고_결제_완료이면_COMPLETED를_반환한다() {
        Product product = createProduct(10);
        User user = createUser("유저1");
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        booking.confirm();
        bookingRepository.save(booking);

        QueueStatusResponse response = queueStatusService.getStatus(product.getId(), user.getId());

        assertThat(response.status()).isEqualTo(QueueStatus.COMPLETED);
    }

    @Test
    void 대기열에서_빠졌고_재고가_있으면_재진입하여_WAITING을_반환한다() {
        Product product = createProduct(10);
        User firstUser = createUser("먼저진입");
        User user = createUser("유저1");
        queueService.enter(product.getId(), firstUser.getId());
        bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        QueueStatusResponse response = queueStatusService.getStatus(product.getId(), user.getId());

        assertAll(
                () -> assertThat(response.status()).isEqualTo(QueueStatus.WAITING),
                () -> assertThat(response.rank()).isEqualTo(2L)
        );
    }

    @Test
    void 대기열에서_빠졌고_재고가_없으면_예외가_발생한다() {
        Product product = createProduct(0);
        User user = createUser("유저1");
        bookingRepository.save(Booking.waiting(user.getId(), product.getId()));

        assertThatThrownBy(() -> queueStatusService.getStatus(product.getId(), user.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 대기열에도_없고_예약도_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> queueStatusService.getStatus(999L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    private Product createProduct(int stock) {
        return productRepository.save(
                new Product("제주 호텔", 100000, stock,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
    }

    private User createUser(String name) {
        return userRepository.save(new User(name, name + "@test.com", 100000L));
    }
}
