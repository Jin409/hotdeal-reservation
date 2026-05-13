-- 부하 테스트용 유저 100명 생성
-- 실행: mysql -h127.0.0.1 -P3307 -uroot -proot reservation < scripts/setup-test-data.sql

INSERT IGNORE INTO users (id, name, email, point_balance)
VALUES
(1, '홍길동', 'hong@test.com', 200000),
(2, '김철수', 'kim@test.com', 200000),
(3, '이영희', 'lee@test.com', 200000);

-- 4~100번 유저
INSERT IGNORE INTO users (id, name, email, point_balance)
SELECT n, CONCAT('user', n), CONCAT('user', n, '@test.com'), 200000
FROM (
    SELECT a.N + b.N * 10 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
         (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
) nums
WHERE n BETWEEN 4 AND 100;