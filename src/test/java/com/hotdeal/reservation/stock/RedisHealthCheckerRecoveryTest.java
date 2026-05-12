package com.hotdeal.reservation.stock;

import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class RedisHealthCheckerRecoveryTest {

    @Autowired
    private RedisHealthChecker redisHealthChecker;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void Redis_장애_감지_후_사용_불가_상태가_된다() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        given(redisTemplate.getConnectionFactory()).willReturn(factory);
        given(factory.getConnection()).willThrow(new RuntimeException("Redis 연결 실패"));

        redisHealthChecker.check();

        assertThat(redisHealthChecker.isRedisAvailable()).isFalse();
    }

    @Test
    void Redis_복구_감지_후_사용_가능_상태로_전환되고_재고가_동기화된다() {
        // 1. 장애 감지
        RedisConnectionFactory failFactory = mock(RedisConnectionFactory.class);
        given(redisTemplate.getConnectionFactory()).willReturn(failFactory);
        given(failFactory.getConnection()).willThrow(new RuntimeException("Redis 연결 실패"));
        redisHealthChecker.check();
        assertThat(redisHealthChecker.isRedisAvailable()).isFalse();

        // 2. 복구 감지
        RedisConnectionFactory recoveredFactory = mock(RedisConnectionFactory.class);
        RedisConnection recoveredConnection = mock(RedisConnection.class);
        given(redisTemplate.getConnectionFactory()).willReturn(recoveredFactory);
        given(recoveredFactory.getConnection()).willReturn(recoveredConnection);
        given(recoveredConnection.ping()).willReturn("PONG");

        redisHealthChecker.check();

        assertThat(redisHealthChecker.isRedisAvailable()).isTrue();
    }
}