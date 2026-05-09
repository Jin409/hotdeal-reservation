package com.hotdeal.reservation.booking;

import com.hotdeal.reservation.booking.dto.BookingRequest;
import com.hotdeal.reservation.booking.dto.PaymentMethodRequest;
import com.hotdeal.reservation.common.AcceptanceTest;
import com.hotdeal.reservation.idempotency.IdempotencyStore;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.stock.StockRedisRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class BookingAcceptanceTest extends AcceptanceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StockRedisRepository stockRedisRepository;

    @Autowired
    private IdempotencyStore idempotencyStore;

    private Product product;
    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
        booking = bookingRepository.save(Booking.waiting(user.getId(), product.getId()));
        stockRedisRepository.set(product.getId(), 10);
    }

    @Test
    void 카드와_포인트_복합결제로_예약에_성공한다() {
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(200)
                .body("bookingId", notNullValue())
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void 동일_멱등성_키로_두번_요청하면_동일_응답을_반환한다() {
        String idempotencyKey = UUID.randomUUID().toString();
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void 신용카드와_Y페이_혼용시_400을_반환한다() {
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPAY", 50000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(400);
    }

    @Test
    void 결제_실패_후_같은_멱등키로_재시도하면_성공한다() {
        String idempotencyKey = UUID.randomUUID().toString();

        // 금액 불일치로 실패
        BookingRequest failRequest = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", idempotencyKey)
                .body(failRequest)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(400);

        // 같은 키로 올바른 금액으로 재시도
        BookingRequest retryRequest = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 50000),
                new PaymentMethodRequest("YPOINT", 50000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", idempotencyKey)
                .body(retryRequest)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void 처리_중인_멱등키로_요청하면_409를_반환한다() {
        String idempotencyKey = UUID.randomUUID().toString();
        idempotencyStore.markProcessing(idempotencyKey);

        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(409);
    }

    @Test
    void 멱등키_없이_요청하면_400을_반환한다() {
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 100000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(400);
    }

    @Test
    void 결제금액이_상품가격과_다르면_400을_반환한다() {
        BookingRequest request = new BookingRequest(product.getId(), List.of(
                new PaymentMethodRequest("CREDIT_CARD", 80000)
        ));

        given()
                .contentType(ContentType.JSON)
                .header("userId", user.getId())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(request)
        .when()
                .post("/bookings/{bookingId}", booking.getId())
        .then()
                .statusCode(400);
    }
}