package com.hotdeal.reservation.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long price;

    @Embedded
    private Stock stock;

    @Column(nullable = false)
    private LocalDateTime checkInAt;

    @Column(nullable = false)
    private LocalDateTime checkOutAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Product(String name, long price, int stock, LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        this.name = name;
        this.price = price;
        this.stock = new Stock(stock);
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.createdAt = LocalDateTime.now();
    }

    public void decreaseStock() {
        this.stock = stock.decrease();
    }
}