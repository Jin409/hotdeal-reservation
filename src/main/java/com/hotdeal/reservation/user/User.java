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

    @Embedded
    private Point point;

    public User(String name, String email, long pointBalance) {
        this.name = name;
        this.email = email;
        this.point = new Point(pointBalance);
    }

    public long getPointBalance() {
        return point.getBalance();
    }

    public void usePoints(int amount) {
        this.point = point.use(amount);
    }
}