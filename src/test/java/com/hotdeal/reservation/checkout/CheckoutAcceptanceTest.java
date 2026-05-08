package com.hotdeal.reservation.checkout;

import com.hotdeal.reservation.product.Product;
import com.hotdeal.reservation.product.ProductRepository;
import com.hotdeal.reservation.user.User;
import com.hotdeal.reservation.user.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Product product;
    private User user;

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        productRepository.deleteAll();
        userRepository.deleteAll();
        product = productRepository.save(
                new Product("제주 호텔", 100000, 10,
                        LocalDateTime.of(2026, 6, 1, 15, 0),
                        LocalDateTime.of(2026, 6, 2, 11, 0))
        );
        user = userRepository.save(new User("홍길동", "hong@test.com", 50000L));
    }

    @Test
    void 주문서_진입시_상품_정보와_포인트를_반환한다() {
        given()
                .param("productId", product.getId())
                .header("userId", user.getId())
        .when()
                .get("/checkout")
        .then()
                .statusCode(200)
                .body("productName", equalTo("제주 호텔"))
                .body("price", equalTo(100000))
                .body("pointBalance", equalTo(50000));
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