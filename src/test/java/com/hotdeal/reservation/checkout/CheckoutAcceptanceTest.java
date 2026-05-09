package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.common.AcceptanceTest;
import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class CheckoutAcceptanceTest extends AcceptanceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
    }

    @Test
    void 주문서_진입시_상품_정보와_포인트와_순번을_반환한다() {
        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(200)
                .body("productName", equalTo("제주 호텔"))
                .body("price", equalTo(100000))
                .body("pointBalance", equalTo(50000))
                .body("rank", notNullValue())
                .body("idempotencyKey", notNullValue());
    }

    @Test
    void 재고가_없으면_400을_반환한다() {
        productRepository.deleteAll();
        product = productRepository.save(
                new Product("제주 호텔", 100000, 0,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(400);
    }

    @Test
    void 동일_유저가_같은_상품에_중복_진입하면_409를_반환한다() {
        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(200);

        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(409);
    }

    @Test
    void 존재하지_않는_상품이면_404를_반환한다() {
        given()
                .param("productId", 999)
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(404);
    }

    @Test
    void 존재하지_않는_사용자이면_404를_반환한다() {
        given()
                .param("productId", product.getId())
                .header("userId", 999)
        .when()
                .get("/checkout")
        .then()
                .statusCode(404);
    }
}