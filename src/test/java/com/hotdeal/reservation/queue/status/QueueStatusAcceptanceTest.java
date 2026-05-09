package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.booking.Booking;
import com.hotdeal.reservation.booking.BookingRepository;
import com.hotdeal.reservation.common.AcceptanceTest;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.queue.QueueService;
import com.hotdeal.reservation.stock.StockRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class QueueStatusAcceptanceTest extends AcceptanceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private StockRedisRepository stockRedisRepository;

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        user = userRepository.save(new User("홍길동", "hong@test.com", 100000L));
    }

    @Test
    void 대기열_첫번째이면_READY를_반환한다() {
        queueService.enter(product.getId(), user.getId());

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/queue-status")
        .then()
                .statusCode(200)
                .body("status", equalTo("READY"))
                .body("rank", equalTo(1));
    }

    @Test
    void 대기열_두번째이면_WAITING을_반환한다() {
        User firstUser = userRepository.save(new User("첫번째", "first@test.com", 100000L));
        queueService.enter(product.getId(), firstUser.getId());
        queueService.enter(product.getId(), user.getId());

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/queue-status")
        .then()
                .statusCode(200)
                .body("status", equalTo("WAITING"))
                .body("rank", equalTo(2));
    }

    @Test
    void 결제_완료이면_COMPLETED를_반환한다() {
        Booking booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        booking.confirm();
        bookingRepository.save(booking);

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/queue-status")
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void 대기열에서_빠졌고_재고가_없으면_400을_반환한다() {
        bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 0);

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/queue-status")
        .then()
                .statusCode(400);
    }

    @Test
    void 대기열에도_없고_예약도_없으면_404를_반환한다() {
        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/queue-status")
        .then()
                .statusCode(404);
    }
}
