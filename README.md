# 선착순 예약 시스템

특정 시간(00시)에 오픈되는 한정 수량 숙소 상품에 대한 선착순 예약 시스템입니다.

## 시스템 아키텍처

```mermaid
graph TD
    Client(["클라이언트"])
    LB["Load Balancer"]
    App1["App Server 1"]
    App2["App Server 2"]
    Redis[("Redis")]
    MySQL[("MySQL")]
    Client --> LB
    LB --> App1
    LB --> App2
    App1 --> Redis
    App2 --> Redis
    App1 --> MySQL
    App2 --> MySQL
```

### 컴포넌트 역할

| 컴포넌트       | 역할                                        |
|------------|-------------------------------------------|
| App Server | 예약/결제 비즈니스 로직 처리 (2대 이상 분산 환경)            |
| Redis      | 대기열 관리, 결제 시간 제한 (ready TTL 3분), 멱등성 키 저장 |
| MySQL      | 상품/유저/예약/결제 영구 저장소, 재고 관리 (비관적 락)         |

### 패키지 구조

```
com.hotdeal.reservation
├── booking/          예약 도메인 (Booking, BookingService, Controller)
├── checkout/         주문서 진입 (CheckoutService, Controller)
├── common/           공통 유틸 (EntityUtils, 예외 클래스)
├── idempotency/      멱등성 처리 (AOP, IdempotencyStore)
├── payment/          결제 도메인
│   ├── client/       PG사 클라이언트 (인터페이스 + Mock)
│   └── processor/    결제 수단별 전략 패턴
├── product/          상품 도메인
├── queue/            대기열 관리
│   └── status/       대기열 상태 조회 (폴링)
├── stock/            재고 관리 (DB 비관적 락)
└── user/             사용자 도메인
```

---

## 실행 방법

### 사전 요구사항

- Java 17
- Docker & Docker Compose

### 1. 인프라 실행

```bash
docker compose up -d
```

MySQL(3307 포트)과 Redis(6379 포트)가 실행됩니다.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

앱 기동 시 자동으로:

- JPA가 테이블을 생성합니다 (`ddl-auto: update`)
- 초기 데이터가 삽입됩니다 (상품 1개, 유저 3명)

### 3. 테스트 실행

```bash
./gradlew test
```

Docker 없이 실행됩니다. H2(인메모리 DB)와 Embedded Redis를 사용합니다.

### 프로파일 구성

| 프로파일         | DB             | Redis          | 용도    |
|--------------|----------------|----------------|-------|
| `local` (기본) | H2 MODE=MySQL  | 비활성화           | 로컬 개발 |
| `prod`       | MySQL (Docker) | Redis (Docker) | 실제 실행 |
| `test`       | H2 MODE=MySQL  | Embedded Redis | 테스트   |

---

## 사용자 흐름

![모바일 화면 흐름](docs/reservation_1.png)

| 화면     | API                   | 설명                        |
|--------|-----------------------|---------------------------|
| 주문서 진입 | `GET /checkout`       | 상품 정보 조회 + 대기열 진입 + 순번 확인 |
| 대기열 폴링 | `GET /queue-status`   | 내 순번 확인, 결제 가능 여부 판단      |
| 예약 완료  | `POST /bookings/{id}` | 결제 처리 + 예약 확정             |

---

## 시퀀스 다이어그램

### 전체 예약 플로우

```mermaid
sequenceDiagram
    actor 사용자
    participant 주문서 as GET /checkout
    participant 폴링 as GET /queue-status
    participant 결제 as POST /bookings/{id}
    participant Redis
    participant DB
    사용자 ->> 주문서: 주문서 진입
    주문서 ->> DB: 상품 정보 + 포인트 조회
    주문서 ->> 주문서: 재고 확인
    주문서 ->> Redis: 대기열 진입
    주문서 ->> Redis: 클라이언트 멱등키 발급
    주문서 ->> DB: 예약 생성 (WAITING)
    주문서 -->> 사용자: 상품 정보 + 순번 + 클라이언트 멱등키

    loop 3~5초 간격 폴링
        사용자 ->> 폴링: 내 순번 확인
        폴링 ->> Redis: 순번 조회
        alt 아직 내 차례가 아님
            폴링 -->> 사용자: WAITING (순번 N)
        else 내 차례
            폴링 -->> 사용자: READY
        end
    end

    사용자 ->> 결제: 결제 요청 (클라이언트 멱등키 포함)
    결제 ->> 결제: 클라이언트 멱등키 중복 확인
    결제 ->> Redis: ready 키 검증 (결제 가능 상태 확인)
    결제 ->> DB: 재고 차감 (비관적 락)
    결제 ->> 결제: 결제 처리 (PG사 호출)
    결제 ->> DB: 결제 저장 + 예약 확정
    결제 ->> Redis: 대기열에서 제거 + ready 키 삭제
    결제 ->> Redis: 다음 사람 ready 마킹 (TTL 3분)
    결제 -->> 사용자: CONFIRMED
    사용자 ->> 폴링: 상태 확인
    폴링 -->> 사용자: COMPLETED
```

### 복합결제 처리 순서

```mermaid
sequenceDiagram
    participant 결제서비스 as PaymentService
    participant 카드 as 카드 PG사
    participant 포인트 as 포인트 잔액
    Note over 결제서비스: 검증 단계
    결제서비스 ->> 결제서비스: 결제 조합 검증 (카드+Y페이 혼용 차단)
    결제서비스 ->> 결제서비스: 총 금액 == 상품 가격 확인
    결제서비스 ->> 결제서비스: 포인트 잔액 충분한지 확인
    Note over 결제서비스: 외부 결제 먼저
    결제서비스 ->> 카드: 카드 결제 요청 (PG 멱등키 포함)
    카드 -->> 결제서비스: 성공
    Note over 결제서비스: 포인트 차감 나중
    결제서비스 ->> 포인트: 포인트 차감
    Note over 결제서비스: 카드 실패 시 포인트를 건드리지 않아<br/>롤백 불필요
```

### 동시 요청 시 재고 선점

```mermaid
sequenceDiagram
    participant 서버1 as App Server 1
    participant 서버2 as App Server 2
    participant DB
    Note over DB: 재고: 1개 남음
    서버1 ->> DB: 재고 조회 (락 획득)
    Note over DB: 서버2는 락 대기
    서버1 ->> DB: 재고 차감 (1 → 0)
    Note over DB: 락 해제
    서버2 ->> DB: 재고 조회 (락 획득)
    DB -->> 서버2: 재고 0
    서버2 -->> 서버2: 재고 없음 예외
    Note over DB: 재고: 0<br/>서버1만 성공, 초과판매 없음
```

### 결제 실패 시 보상 트랜잭션

```mermaid
sequenceDiagram
    participant 예약서비스
    participant PG사
    participant DB
    예약서비스 ->> DB: 재고 차감 (비관적 락)
    DB -->> 예약서비스: 성공
    예약서비스 ->> PG사: 결제 요청
    PG사 -->> 예약서비스: 카드 거절 (NonRetryable)
    Note over 예약서비스: 보상 트랜잭션 시작
    예약서비스 ->> DB: 예약 상태 → CANCELLED (별도 트랜잭션)
    예약서비스 ->> 예약서비스: 클라이언트 멱등키 삭제 (재시도 허용)
    예약서비스 -->> 예약서비스: 에러 반환
```

### 결제 재시도 (PG사 일시 장애)

```mermaid
sequenceDiagram
    participant 결제처리기
    participant PG사
    결제처리기 ->> PG사: 1차 결제 요청
    PG사 -->> 결제처리기: 일시 장애 (Retryable)
    Note over 결제처리기: 1초 대기
    결제처리기 ->> PG사: 2차 결제 요청 (동일 PG 멱등키)
    PG사 -->> 결제처리기: 일시 장애 (Retryable)
    Note over 결제처리기: 2초 대기
    결제처리기 ->> PG사: 3차 결제 요청 (동일 PG 멱등키)
    alt 성공
        PG사 -->> 결제처리기: 성공
    else 최종 실패
        PG사 -->> 결제처리기: 실패
        Note over 결제처리기: 보상 트랜잭션 실행<br/>(예약 취소)
    end
```

### 대기열 이탈 처리 (ready TTL 만료)

```mermaid
sequenceDiagram
    actor 유저1 as 유저1 (이탈)
    actor 유저2 as 유저2 (대기 중)
    participant 폴링 as GET /queue-status
    participant Redis
    participant 스케줄러
    Note over Redis: 유저1 rank 1 (ready TTL 3분)
    Note over Redis: 유저2 rank 2
    유저1 ->> 유저1: 결제하지 않고 이탈
    Note over Redis: 3분 경과 → ready 키 자동 만료

    alt 유저2가 폴링 (lazy)
        유저2 ->> 폴링: 내 순번 확인
        폴링 ->> Redis: rank 1의 ready 키 확인
        Redis -->> 폴링: 없음 (만료됨)
        폴링 ->> Redis: 유저1 대기열에서 제거
        폴링 ->> Redis: 유저2 ready 마킹 (TTL 3분)
        폴링 -->> 유저2: READY
    else 스케줄러 감지 (proactive, 5초 간격)
        스케줄러 ->> Redis: 대기열 있는 상품 조회
        스케줄러 ->> Redis: rank 1의 ready 키 확인
        Redis -->> 스케줄러: 없음 (만료됨)
        스케줄러 ->> Redis: 유저1 대기열에서 제거
        스케줄러 ->> Redis: 유저2 ready 마킹 (TTL 3분)
    end
```

### Redis 장애 시 Checkout

```mermaid
sequenceDiagram
    actor 사용자
    participant 주문서 as GET /checkout
    participant Redis
    participant DB
    Note over Redis: Redis 장애 상황
    사용자 ->> 주문서: 주문서 진입
    주문서 ->> DB: 상품 정보 + 포인트 조회
    주문서 ->> 주문서: 재고 확인
    주문서 ->> Redis: 대기열 진입 시도
    Redis -->> 주문서: 연결 실패 → 건너뜀
    주문서 ->> Redis: 클라이언트 멱등키 발급 시도
    Redis -->> 주문서: 연결 실패 → 건너뜀
    주문서 ->> DB: 예약 생성 (WAITING)
    주문서 -->> 사용자: 상품 정보 + bookingId (순번 없음, 클라이언트 멱등키 없음)
```

### Redis 장애 시 결제

```mermaid
sequenceDiagram
    actor 사용자
    participant 결제 as POST /bookings/{id}
    participant Redis
    participant DB
    Note over Redis: Redis 장애 상황
    사용자 ->> 결제: 결제 요청 (멱등키 없이)
    결제 ->> 결제: 클라이언트 멱등키 없음 → 멱등성 체크 건너뜀
    결제 ->> DB: 예약 조회 → WAITING 확인
    결제 ->> Redis: ready 키 검증 시도
    Redis -->> 결제: 연결 실패 → 건너뜀
    결제 ->> DB: 재고 차감 (비관적 락)
    결제 ->> 결제: 결제 처리 (PG사 호출)
    결제 ->> DB: 결제 저장 + 예약 확정 (CONFIRMED)
    결제 -->> 사용자: CONFIRMED
    Note over 결제: 중복 요청 방어
    사용자 ->> 결제: 같은 bookingId로 재요청
    결제 ->> DB: 예약 조회 → 이미 CONFIRMED
    결제 -->> 사용자: 400 (이미 완료된 예약)
```

---

## ERD

```mermaid
erDiagram
    users {
        bigint id PK
        varchar name
        varchar email
        bigint point_balance
    }

    products {
        bigint id PK
        varchar name
        bigint price
        int stock
        datetime check_in_at
        datetime check_out_at
        datetime created_at
    }

    bookings {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        varchar status
        datetime created_at
    }

    payments {
        bigint id PK
        bigint booking_id FK
        bigint total_amount
        varchar status
        datetime created_at
    }

    payment_items {
        bigint id PK
        bigint payment_id FK
        varchar payment_type
        bigint amount
    }

    users ||--o{ bookings: "예약"
    products ||--o{ bookings: "상품"
    bookings ||--|| payments: "결제"
    payments ||--o{ payment_items: "결제 수단"
```

### 테이블 설명

| 테이블             | 설명                                           |
|-----------------|----------------------------------------------|
| `users`         | 사용자 정보. 포인트 잔액 포함                            |
| `products`      | 숙소 상품. 재고(stock)는 DB 비관적 락으로 관리              |
| `bookings`      | 예약 내역. 상태: WAITING → CONFIRMED / CANCELLED   |
| `payments`      | 결제 내역. 상태: PENDING → SUCCESS                 |
| `payment_items` | 복합결제의 각 결제 수단별 금액 (카드 80,000 + 포인트 20,000 등) |

### Redis 키 목록

| 키                                           | 자료구조   | TTL | 용도                               |
|---------------------------------------------|--------|-----|----------------------------------|
| `queue:product:{productId}`                 | List   | 없음  | 대기열 순서 관리                        |
| `queue:entered:{productId}`                 | Set    | 없음  | 중복 진입 방지                         |
| `queue:ready:{productId}:{userId}`          | String | 3분  | rank 1 결제 가능 상태                  |
| `idempotency:{key}`                         | String | 10분 | 클라이언트 멱등키 (PROCESSING → 응답 JSON) |
| `idempotency:checkout:{productId}:{userId}` | String | 10분 | 주문서 진입 시 멱등키 발급 저장               |

---

## API 목록

### GET /checkout

주문서 진입. 상품 정보 조회 + 대기열 진입 + 멱등키 발급.

**Request**

```
GET /checkout?productId=1
Header: userId: 1
```

**Response (200)**

```json
{
  "bookingId": 1,
  "idempotencyKey": "uuid-abc-123",
  "productName": "제주 초특가 호텔",
  "price": 100000,
  "checkInAt": "2026-06-01T15:00:00",
  "checkOutAt": "2026-06-02T11:00:00",
  "pointBalance": 50000,
  "rank": 1
}
```

| 상태 코드 | 설명              |
|-------|-----------------|
| 200   | 정상              |
| 400   | 재고 없음           |
| 404   | 상품/사용자 없음       |
| 409   | 이미 대기열에 진입한 사용자 |

---

### GET /queue-status

대기열 상태 조회. 3~5초 간격으로 폴링.

**Request**

```
GET /queue-status?productId=1
Header: userId: 1
```

**Response (200)**

```json
{
  "status": "WAITING",
  "rank": 5
}
{
  "status": "READY",
  "rank": 1
}
{
  "status": "COMPLETED",
  "rank": null
}
```

| 상태 코드 | 설명                               |
|-------|----------------------------------|
| 200   | 정상 (WAITING / READY / COMPLETED) |
| 400   | 재고 없음 (재진입 불가)                   |
| 404   | 대기열에 없는 사용자                      |
| 503   | Redis 장애로 조회 불가                  |

---

### POST /bookings/{bookingId}

결제 및 예약 완료.

**Request**

```
POST /bookings/1
Header: userId: 1
Header: Idempotency-Key: uuid-abc-123
Content-Type: application/json

{
  "productId": 1,
  "paymentMethods": [
    { "type": "CREDIT_CARD", "amount": 80000 },
    { "type": "YPOINT", "amount": 20000 }
  ]
}
```

**Response (200)**

```json
{
  "bookingId": 1,
  "status": "CONFIRMED"
}
```

| 상태 코드 | 설명                                                 |
|-------|----------------------------------------------------|
| 200   | 결제 성공                                              |
| 400   | 결제 검증 실패 (혼용, 금액 불일치, 포인트 부족, 재고 없음, 순번 아님, 이미 완료) |
| 404   | 예약 없음                                              |
| 409   | 처리 중 (멱등키 충돌)                                      |
| 500   | PG사 장애 (재시도 모두 실패)                                 |

**지원 결제 수단**

| 결제 수단         | 설명              |
|---------------|-----------------|
| `CREDIT_CARD` | 신용카드 (PG사 호출)   |
| `YPAY`        | Y페이 (PG사 호출)    |
| `YPOINT`      | Y포인트 (내부 잔액 차감) |

**복합결제 규칙**

- 카드 + 포인트 ✅
- Y페이 + 포인트 ✅
- 카드 + Y페이 ❌ (혼용 불가)