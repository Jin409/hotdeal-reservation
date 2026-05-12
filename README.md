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

| 컴포넌트 | 역할 |
|---|---|
| App Server | 예약/결제 비즈니스 로직 처리 (2대 이상 분산 환경) |
| Redis | 대기열 관리, 재고 선점(원자적 감소), 멱등성 키 저장 |
| MySQL | 상품/유저/예약/결제 영구 저장소 |

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
├── stock/            재고 관리 (Redis + DB Fallback)
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
- Redis에 재고가 동기화됩니다

### 3. 테스트 실행

```bash
./gradlew test
```

Docker 없이 실행됩니다. H2(인메모리 DB)와 Embedded Redis를 사용합니다.

### 프로파일 구성

| 프로파일 | DB | Redis | 용도 |
|---|---|---|---|
| `local` (기본) | H2 MODE=MySQL | 비활성화 | 로컬 개발 |
| `prod` | MySQL (Docker) | Redis (Docker) | 실제 실행 |
| `test` | H2 MODE=MySQL | Embedded Redis | 테스트 |