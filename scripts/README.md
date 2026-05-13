# 부하 테스트 가이드

k6 기반 부하 테스트로 동시성과 전체 플로우를 검증합니다.

## 사전 요구사항

```bash
# k6 설치 (Mac)
brew install k6
```

## 테스트 실행

### 1. 인프라 + 앱 실행

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 2. 테스트 데이터 생성

```bash
mysql -h127.0.0.1 -P3307 -uroot -proot reservation < scripts/setup-test-data.sql
```

### 3. 부하 테스트 실행

```bash
k6 run scripts/load-test.js
```

### 4. 결과 확인

```bash
# DB 재고 확인 (10 - 예약 성공 수)
mysql -h127.0.0.1 -P3307 -uroot -proot reservation -e "SELECT stock FROM products WHERE id=1;"

# 예약 상태별 개수
mysql -h127.0.0.1 -P3307 -uroot -proot reservation -e "SELECT status, COUNT(*) FROM bookings GROUP BY status;"
```

## 테스트 시나리오

### 동시 100명 전체 플로우

100명의 유저가 동시에 checkout → polling → booking을 진행합니다.

```
VU 100명 동시 시작
→ GET /checkout (대기열 진입)
→ GET /queue-status (폴링, rank 1이 될 때까지)
→ POST /bookings (결제)
```

### 기대 결과

| 항목 | 기대값 |
|---|---|
| checkout 성공 | 100명 |
| booking 성공 | 10명 (재고 10개) |
| booking 실패 | 90명 (재고 소진) |
| DB 재고 | 0 |
| 초과판매 | 없음 |

### 커스텀 메트릭

| 메트릭 | 설명 |
|---|---|
| `checkout_success` | checkout 성공 수 |
| `checkout_fail` | checkout 실패 수 |
| `booking_success` | 결제 성공 수 |
| `booking_fail` | 결제 실패 수 (재고 소진 또는 폴링 타임아웃) |

## 환경 변수

```bash
# 다른 호스트로 테스트
k6 run -e BASE_URL=http://192.168.1.100:8080 scripts/load-test.js
```