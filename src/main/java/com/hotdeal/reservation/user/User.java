package com.hotdeal.reservation.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Long pointBalance;

    public User(String name, String email, Long pointBalance) {
        this.name = name;
        this.email = email;
        this.pointBalance = pointBalance;
    }

    public void usePoints(int amount) {
        if (this.pointBalance < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.pointBalance -= amount;
    }
}