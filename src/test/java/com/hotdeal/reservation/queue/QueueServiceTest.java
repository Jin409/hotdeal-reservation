package com.hotdeal.reservation.queue;

import com.hotdeal.reservation.common.ServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class QueueServiceTest extends ServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void clearRedis() {
        redisTemplate.delete(QueueKeys.queue(1L));
        redisTemplate.delete(QueueKeys.entered(1L));
    }

    @Test
    void 대기열에_진입하면_순번을_반환한다() {
        Long rank = queueService.enter(1L, 10L);

        assertThat(rank).isEqualTo(1L);
    }

    @Test
    void 여러_유저가_순서대로_진입하면_순번이_증가한다() {
        Long rank1 = queueService.enter(1L, 10L);
        Long rank2 = queueService.enter(1L, 20L);
        Long rank3 = queueService.enter(1L, 30L);

        assertAll(
                () -> assertThat(rank1).isEqualTo(1L),
                () -> assertThat(rank2).isEqualTo(2L),
                () -> assertThat(rank3).isEqualTo(3L)
        );
    }

    @Test
    void 동일_유저가_중복_진입하면_예외가_발생한다() {
        queueService.enter(1L, 10L);

        assertThatThrownBy(() -> queueService.enter(1L, 10L))
                .isInstanceOf(DuplicateEntryException.class);
    }

    @Test
    void 중복_진입_시_대기열에_추가되지_않는다() {
        queueService.enter(1L, 10L);

        try {
            queueService.enter(1L, 10L);
        } catch (DuplicateEntryException ignored) {
        }

        Long size = redisTemplate.opsForList().size(QueueKeys.queue(1L));
        assertThat(size).isEqualTo(1L);
    }
}