# hotdeal-reservation

## 프로젝트 개요
핫딜 예약 서비스 (Spring Boot 3.5.0, Java 17)

## 기술 스택
- Spring Boot 3.5.0 (Web, JPA, Redis, Validation, AOP)
- MySQL 8.0, Redis 7
- Testcontainers, RestAssured, H2 (테스트)
- 로컬: H2 MODE=MySQL, CI: Testcontainers

## Skills
- 테스트 작성: `skills/test.md`
- PR 본문 작성: `skills/pr.md`

핵심 규칙:
- 테스트 메서드명은 한글 + 언더스코어 (`재고가_없으면_예약에_실패한다`)
- 단위 테스트: 순수 Java, Spring 컨텍스트 없음, AssertJ만 사용
- 서비스 테스트: SpringBootTest + Testcontainers (MySQL, Redis)
- 인수 테스트: RestAssured + RANDOM_PORT
- PgClient만 MockBean 허용, 나머지는 실제 연동