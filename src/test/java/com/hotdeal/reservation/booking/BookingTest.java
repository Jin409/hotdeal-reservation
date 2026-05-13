package com.hotdeal.reservation.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    @Test
    void WAITING_상태에서는_검증을_통과한다() {
        Booking booking = Booking.waiting(1L, 1L);

        booking.validateNotCompleted();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void CONFIRMED_상태에서는_검증에_실패한다() {
        Booking booking = Booking.waiting(1L, 1L);
        booking.confirm();

        assertThatThrownBy(booking::validateNotCompleted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료된 예약");
    }

    @Test
    void confirm_호출시_CONFIRMED_상태가_된다() {
        Booking booking = Booking.waiting(1L, 1L);

        booking.confirm();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancel_호출시_CANCELLED_상태가_된다() {
        Booking booking = Booking.waiting(1L, 1L);

        booking.cancel();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}