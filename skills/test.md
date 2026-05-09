---
description: JUnit5 + Testcontainers 기반 테스트 작성 가이드. 단위(Domain) / 서비스(Service) / 인수(Acceptance) 테스트 작성 시, 동시성 테스트 작성 시 참고합니다.
---

# 테스트 작성 가이드

## 레이어 구조

| 레이어 | 대상 | 특징 |
|---|---|---|
| 단위 (Domain) | 엔티티, 검증 로직 | 순수 Java, Spring 컨텍스트 없음 |
| 서비스 (Service) | Service, Repository | SpringBootTest + Testcontainers |
| 인수 (Acceptance) | API 전체 플로우 | RestAssured + 실제 HTTP |

---

## 1. 단위 테스트 (Domain)

### 위치
```
프로덕션 코드와 동일한 패키지 경로에 위치합니다.
예: src/main/.../user/Point.java → src/test/.../user/PointTest.java
```

### 규칙
- Spring 컨텍스트 없이 순수 Java로 작성합니다.
- 외부 의존성(Redis, DB, PG사)을 사용하지 않습니다.
- AssertJ만 사용합니다. (`assertThat`, `assertAll`, `assertThatThrownBy`)
- Mockito를 사용하지 않습니다. 도메인 로직만 검증합니다.

### 대상
- 도메인 엔티티 생성 및 검증 로직
- PaymentProcessor 각 구현체
- 복합결제 검증 (CREDIT_CARD + YPAY 혼용 차단)
- Exception 분류 로직

### 예시
```java
class PaymentValidatorTest {

    @Test
    void 신용카드와_Y페이_혼용시_검증에_실패한다() {
        List<PaymentMethod> methods = List.of(
            new PaymentMethod(PaymentType.CREDIT_CARD, 50000),
            new PaymentMethod(PaymentType.YPAY, 30000)
        );

        assertThatThrownBy(() -> PaymentValidator.validate(methods))
            .isInstanceOf(InvalidPaymentCombinationException.class);
    }
}
```

---

## 2. 서비스 테스트 (Service)

### 베이스 클래스: `ServiceTest`
```java
@SpringBootTest
@ActiveProfiles("test")
public abstract class ServiceTest {
    // Testcontainers MySQL + Redis 실제 연동
}
```

### Testcontainers 설정
- MySQL, Redis 컨테이너를 static으로 선언하여 테스트 클래스 간 재사용합니다.
- 컨테이너를 재사용하지 않으면 매 테스트마다 기동 시간이 발생합니다.
- Redis는 `com.redis:testcontainers-redis` 전용 라이브러리를 사용합니다.

```java
@TestConfiguration
public class TestContainersConfig {

    static final MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8.0").withDatabaseName("reservation");

    static final RedisContainer redis =
        new RedisContainer(DockerImageName.parse("redis:7"));

    static {
        mysql.start();
        redis.start();
    }
}
```

### 규칙
- `@Transactional`을 테스트에 붙이지 않습니다. (실제 코드의 트랜잭션 누락을 감지하기 위함)
- `@AfterEach`로 데이터를 정리합니다.
- 여러 값을 검증할 때는 `assertAll`로 묶어서 한번에 검증합니다.
- `PgClient`만 `@MockBean` 허용합니다. (실제 PG사 연동 불가)
- Redis는 Embedded Redis(`com.github.codemonstur:embedded-redis`)를 사용합니다. MockBean 불필요.
- `ServiceTest`, `AcceptanceTest` 베이스 클래스를 상속하면 DB 정리 + Redis 정리가 자동으로 됩니다.
- 데이터 초기화는 `@Sql(scripts = "/fixture.sql")`로 주입합니다.
- fixture.sql 위치: `src/test/resources/fixture.sql`

### 동시성 테스트 패턴
- `CountDownLatch` + `ExecutorService`로 동시 요청을 시뮬레이션합니다.
- 재고 10개 기준, 100명 동시 요청 시 정확히 10명만 성공하는지 검증합니다.

```java
@Test
void 동시에_100명이_요청해도_10명만_예약에_성공한다() throws InterruptedException {
    int threadCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try { bookingService.book(request); }
            finally { latch.countDown(); }
        });
    }

    latch.await();
    assertThat(bookingRepository.countConfirmed()).isEqualTo(10);
}
```

---

## 3. 인수 테스트 (Acceptance)

### 베이스 클래스: `AcceptanceTest`
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AcceptanceTest {

    @LocalServerPort int port;

    @MockBean PgClient pgClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

### 헬퍼 메서드
AcceptanceTest 베이스 클래스에 아래 헬퍼 메서드를 정의합니다.

```java
CheckoutResponse checkout(Long productId, Long userId)
BookingResponse book(String idempotencyKey, BookingRequest request)
QueueStatusResponse pollUntilDone(Long bookingId)  // CONFIRMED or FAILED까지 폴링
```

### 대상 시나리오
- Checkout → Booking → 폴링 정상 플로우
- 동일 멱등성 키로 두 번 요청 시 동일 응답 반환
- CREDIT_CARD + YPAY 혼용 시 400 응답
- 매진 시 실패 응답

---

## 네이밍 컨벤션

- 테스트 메서드명은 한글로 작성합니다.
- `언더스코어`로 단어를 구분합니다.

```
✅ 재고가_없으면_예약에_실패한다
✅ 동시에_100명이_요청해도_10명만_성공한다
✅ 동일_멱등성_키로_두번_요청하면_동일_응답을_반환한다
❌ testBookingFailWhenSoldOut
```

---

## 테스트 인프라

### DB
- H2 MODE=MySQL 사용 (Docker 불필요)
- `DatabaseCleaner`가 매 테스트 후 전체 테이블 TRUNCATE

### Redis
- Embedded Redis 사용 (Docker 불필요)
- `EmbeddedRedisConfig`가 랜덤 포트로 Redis 서버 기동
- `ServiceTest`, `AcceptanceTest`에서 `@Import(EmbeddedRedisConfig.class)` 적용
- 매 테스트 후 `flushAll`로 Redis 정리

### 주의사항
H2 MODE=MySQL이 완벽하지 않으므로 MySQL 전용 문법 사용을 금지합니다.
```
X ON DUPLICATE KEY UPDATE
X DATE_FORMAT 등 MySQL 전용 함수
```