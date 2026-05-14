package com.hotdeal.reservation.queue;

import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.exception.DuplicateEntryException;
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
    private QueueRedisRepository queueRedisRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
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
    void 동일_유저가_중복_진입하면_예외가_발생하고_대기열에_추가되지_않는다() {
        queueService.enter(1L, 10L);

        assertThatThrownBy(() -> queueService.enter(1L, 10L))
                .isInstanceOf(DuplicateEntryException.class);

        Long size = redisTemplate.opsForList().size(QueueKeys.queue(1L));
        assertThat(size).isEqualTo(1L);
    }

    @Test
    void 첫번째_진입자는_즉시_ready_상태가_된다() {
        queueService.enter(1L, 10L);

        assertThat(queueRedisRepository.isReady(1L, 10L)).isTrue();
    }

    @Test
    void 두번째_진입자는_ready_상태가_아니다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        assertThat(queueRedisRepository.isReady(1L, 20L)).isFalse();
    }

    @Test
    void 첫번째가_떠나면_두번째가_ready_상태가_된다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        queueService.leave(1L, 10L);

        assertAll(
                () -> assertThat(queueRedisRepository.isReady(1L, 10L)).isFalse(),
                () -> assertThat(queueRedisRepository.isReady(1L, 20L)).isTrue()
        );
    }

    @Test
    void ready_만료된_첫번째_사용자가_대기열에서_제거되고_다음_사람이_승격된다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        // ready 키 수동 삭제 (TTL 만료 시뮬레이션)
        queueRedisRepository.removeReady(1L, 10L);

        queueService.evictReadyExpiredUser(1L);

        assertAll(
                () -> assertThat(queueRedisRepository.getRank(1L, 10L)).isNull(),
                () -> assertThat(queueRedisRepository.getRank(1L, 20L)).isEqualTo(1L),
                () -> assertThat(queueRedisRepository.isReady(1L, 20L)).isTrue()
        );
    }

    @Test
    void leave가_동시에_호출되어도_다른_사용자가_제거되지_않는다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        queueService.leave(1L, 10L);
        queueService.leave(1L, 10L);

        assertAll(
                () -> assertThat(queueRedisRepository.getRank(1L, 20L)).isEqualTo(1L),
                () -> assertThat(queueRedisRepository.isReady(1L, 20L)).isTrue()
        );
    }

    @Test
    void 대기열에_진입하면_active_products에_추가된다() {
        queueService.enter(1L, 10L);

        assertThat(queueRedisRepository.getActiveProductIds()).contains(1L);
    }

    @Test
    void 모든_사용자가_떠나면_active_products에서_제거된다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        queueService.leave(1L, 10L);
        queueService.leave(1L, 20L);

        assertThat(queueRedisRepository.getActiveProductIds()).doesNotContain(1L);
    }

    @Test
    void ready가_유효한_첫번째_사용자는_제거되지_않는다() {
        queueService.enter(1L, 10L);
        queueService.enter(1L, 20L);

        queueService.evictReadyExpiredUser(1L);

        assertAll(
                () -> assertThat(queueRedisRepository.getRank(1L, 10L)).isEqualTo(1L),
                () -> assertThat(queueRedisRepository.getRank(1L, 20L)).isEqualTo(2L)
        );
    }
}