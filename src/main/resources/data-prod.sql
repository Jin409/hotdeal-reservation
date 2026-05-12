INSERT IGNORE INTO products (id, name, price, stock, check_in_at, check_out_at, created_at)
VALUES (1, '제주 초특가 호텔', 100000, 10, '2026-06-01 15:00:00', '2026-06-02 11:00:00', NOW());

INSERT IGNORE INTO users (id, name, email, point_balance)
VALUES (1, '홍길동', 'hong@test.com', 50000),
       (2, '김철수', 'kim@test.com', 100000),
       (3, '이영희', 'lee@test.com', 30000);