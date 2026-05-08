package com.hotdeal.reservation.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private int amount;

    public PaymentItem(Long paymentId, PaymentType paymentType, int amount) {
        this.paymentId = paymentId;
        this.paymentType = paymentType;
        this.amount = amount;
    }
}