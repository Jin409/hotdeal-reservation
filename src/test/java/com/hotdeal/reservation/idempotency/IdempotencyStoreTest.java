package com.hotdeal.reservation.idempotency;

import com.hotdeal.reservation.common.ServiceTest;
import com.hotdeal.reservation.common.idempotency.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class IdempotencyStoreTest extends ServiceTest {

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Test
    void 처리_중_마킹_후_조회하면_PROCESSING을_반환한다() {
        idempotencyStore.markProcessing("key-1");

        Optional<String> result = idempotencyStore.find("key-1");

        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(idempotencyStore.isProcessing(result.get())).isTrue()
        );
    }

    @Test
    void 응답_저장_후_조회하면_JSON을_반환한다() {
        idempotencyStore.markProcessing("key-2");
        idempotencyStore.save("key-2", "{\"status\":\"CONFIRMED\"}");

        Optional<String> result = idempotencyStore.find("key-2");

        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(idempotencyStore.isProcessing(result.get())).isFalse(),
                () -> assertThat(result.get()).contains("CONFIRMED")
        );
    }

    @Test
    void 동일_키로_두번_마킹하면_두번째는_실패한다() {
        boolean first = idempotencyStore.markProcessing("key-3");
        boolean second = idempotencyStore.markProcessing("key-3");

        assertAll(
                () -> assertThat(first).isTrue(),
                () -> assertThat(second).isFalse()
        );
    }

    @Test
    void 키를_삭제하면_조회되지_않는다() {
        idempotencyStore.markProcessing("key-4");
        idempotencyStore.delete("key-4");

        Optional<String> result = idempotencyStore.find("key-4");

        assertThat(result).isEmpty();
    }

    @Test
    void 삭제_후_같은_키로_다시_마킹할_수_있다() {
        idempotencyStore.markProcessing("key-5");
        idempotencyStore.delete("key-5");

        boolean result = idempotencyStore.markProcessing("key-5");

        assertThat(result).isTrue();
    }

    @Test
    void 멱등키를_발급하면_UUID를_반환한다() {
        String key = idempotencyStore.issue("1:10");

        assertThat(key).isNotNull();
    }

    @Test
    void 동일_ID로_발급하면_같은_멱등키를_반환한다() {
        String first = idempotencyStore.issue("1:10");
        String second = idempotencyStore.issue("1:10");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void 다른_ID로_발급하면_다른_멱등키를_반환한다() {
        String key1 = idempotencyStore.issue("1:10");
        String key2 = idempotencyStore.issue("1:20");

        assertThat(key1).isNotEqualTo(key2);
    }
}