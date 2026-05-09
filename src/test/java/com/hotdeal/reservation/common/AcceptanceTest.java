package com.hotdeal.reservation.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(EmbeddedRedisConfig.class)
public abstract class AcceptanceTest {

    @LocalServerPort
    protected int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUpPort() {
        RestAssured.port = port;
    }

    @AfterEach
    void cleanUp() {
        databaseCleaner.clear();
    }
}