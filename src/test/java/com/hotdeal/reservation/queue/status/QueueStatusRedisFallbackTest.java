package com.hotdeal.reservation.queue.status;

import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import com.hotdeal.reservation.common.exception.ServiceUnavailableException;
import com.hotdeal.reservation.queue.QueueRedisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class QueueStatusRedisFallbackTest {

    @Autowired
    private QueueStatusService queueStatusService;

    @MockitoBean
    private QueueRedisRepository queueRedisRepository;

    @Test
    void Redis_장애시_503을_반환한다() {
        given(queueRedisRepository.getRank(anyLong(), anyLong()))
                .willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        assertThatThrownBy(() -> queueStatusService.getStatus(1L, 1L))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}