package com.hotdeal.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class HotdealReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotdealReservationApplication.class, args);
	}

}
