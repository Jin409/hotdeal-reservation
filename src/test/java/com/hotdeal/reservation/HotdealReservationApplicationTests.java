package com.hotdeal.reservation;

import com.hotdeal.reservation.common.EmbeddedRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class HotdealReservationApplicationTests {

	@Test
	void contextLoads() {
	}
}