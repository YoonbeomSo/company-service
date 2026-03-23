# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.yoonbeom.companyservice.SomeTestClass"

# Run a single test method
./gradlew test --tests "com.yoonbeom.companyservice.SomeTestClass.methodName"

# Clean build
./gradlew clean build
```

## Tech Stack

- **Language:** Kotlin 2.2.21 on Java 21
- **Framework:** Spring Boot 4.0.4 (Spring Web MVC)
- **ORM:** Spring Data JPA with Hibernate
- **Validation:** Spring Boot Starter Validation (Jakarta)
- **Database:** MySQL
- **Build:** Gradle 9.4.0 (Kotlin DSL)
- **Testing:** JUnit 5 with Spring Boot Test

## Architecture

Standard Spring Boot layered architecture under `com.yoonbeom.companyservice`:

- **Base package:** `src/main/kotlin/com/yoonbeom/companyservice/`
- **Config:** `src/main/resources/application.properties`

JPA entities must use `var` properties (not `val`) — the `allopen` plugin is configured for `@Entity`, `@MappedSuperclass`, and `@Embeddable`.

Kotlin compiler uses `-Xjsr305=strict` for null-safety interop with Spring annotations and `-Xannotation-default-target=param-property` for constructor parameter annotation targeting.




## 레이어 아키텍처 (commerce-api 기준)

```
interfaces/api/     → Controller, ApiSpec (Swagger), DTO
application/        → Service (@Transactional), Facade (2+ 서비스 조합 시만 고려하기), Criteria
domain/             → Entity, Repository Interface, DomainService, Command (핵심 도메인, 프레임워크 비의존)
infrastructure/     → Repository 구현체, 외부 연동
support/            → 공통 유틸, 에러 처리
```

### 코드 패턴 예시

```kotlin
// ApiSpec — Swagger 어노테이션 담당
@Tag(name = "Xxx V1 API", description = "도메인 API")
interface XxxV1ApiSpec { ... }

// 단일 서비스: Controller → Service 직접 호출
@RestController
class XxxV1Controller(private val xxxService: XxxService) : XxxV1ApiSpec

// 2+ 서비스 조합: Controller → Facade 호출
@RestController
class XxxAdminV1Controller(
    private val xxxService: XxxService,    // 대부분 메서드
    private val xxxFacade: XxxFacade,      // 교차 도메인 메서드만
) : XxxAdminV1ApiSpec

@Component
class XxxService(private val xxxRepository: XxxRepository)  // @Transactional은 여기에

@Component
class XxxFacade(private val xxxService: XxxService, private val yyyService: YyyService)
// @Transactional은 여기에 (교차 도메인 원자성 보장)
```

## 도메인 & 객체 설계 전략

- 도메인 객체는 비즈니스 규칙을 캡슐화해야 합니다.
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공해야 합니다.
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높습니다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행합니다.

## 아키텍처, 패키지 구성 전략

- 본 프로젝트는 레이어드 아키텍처를 따르며, DIP (의존성 역전 원칙) 을 준수합니다.
- API request, response DTO와 응용 레이어의 DTO는 분리해 작성하도록 합니다.
- 패키징 전략은 4개 레이어 패키지를 두고, 하위에 도메인 별로 패키징하는 형태로 작성합니다.
    - `/interfaces/api` (presentation 레이어 - API)
    - `/application/..` (application 레이어 - 도메인 레이어를 조합해 사용 가능한 기능을 제공)
    - `/domain/..` (domain 레이어 - 도메인 객체 및 엔티티, Repository 인터페이스가 위치)
    - `/infrastructure/..` (infrastructure 레이어 - JPA, Redis 등을 활용해 Repository 구현체를 제공)

## 개발 규칙

### 진행 Workflow - 증강 코딩

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행
- **중간 결과 보고**: AI가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입
- **설계 주도권 유지**: AI가 임의판단을 하지 않고, 방향성에 대한 제안을 진행할 수 있으나 개발자의 승인을 받은 후 수행

### 개발 Workflow - TDD

- Red → Green → Refactor 순서로 개발한다.
- 테스트는 Arrange - Act - Assert (3A) 구조로 작성하고 주석으로 구분한다.
- 메서드명은 영어 camelCase `결과_when조건`, `@DisplayName`은 한글로 작성한다.
- 기능별 `@Nested`로 그룹핑한다.
- 정상 흐름, 예외 흐름, 경계값 케이스를 반드시 포함한다.
- 도메인별로 3가지 테스트를 반드시 작성한다:
    - **단위 테스트**: 도메인 엔티티는 Mock 없이 순수 인스턴스화, 서비스는 Mockito 사용 (`@ExtendWith(MockitoExtension::class)`)
    - **통합 테스트**: `@SpringBootTest` + 실제 DB(Testcontainers)로 Service → Repository 레이어 통합 검증, Mock 없이 실제 저장/조회 (`@Transactional` 경계인 Service 기준, 2+ 서비스 조합 시 Facade 기준)
    - **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`으로 HTTP 요청/응답 검증


## 주의사항

### 1. Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현을 하지 말 것
- null-safety 하지 않게 코드 작성하지 말 것 (Kotlin의 null-safety 활용)
- `println` 코드 남기지 말 것
- 테스트를 임의로 삭제하거나 `@Disabled` 처리하지 말 것
- 요청하지 않은 기능을 임의로 구현하지 말 것

### 2. Recommendation
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `.http/*.http`에 분류해 작성
- 기존 코드 패턴 분석 후 일관성 유지

### 3. Priority
- 실제 동작하는 해결책만 고려
- null-safety, thread-safety 고려
- 테스트 가능한 구조로 설계
- 기존 코드 패턴 분석 후 일관성 유지

### 4. 개발자 정보
- Kotlin 코드 작성 시 Java와 다른 핵심 기능이 있으면 상세히 설명할 것
- Kotlin 고유 기능을 사용할 경우, 왜 사용하는지 설명할 것

### 5. git
- commit, push 등 git에 관련된 명령어는 개발자의 확인을 받을 것
- 커밋 메시지 형식:
  ```
  <type>: <subject>

  - <변경 내용 1>
  - <변경 내용 2>
  ```
    - `type`: `feat`, `fix`, `docs`, `refactor`, `test`, `chore` 등
    - `subject`: 변경 대상 요약
    - 본문: 빈 줄 후, 변경 코드 내용이 아닌 핵심 변경 내용을 `-`로 나열
- Co-Authored-By: ~ 내용 절대 커밋 내용에 포함시키지마


## 빌드 및 실행 명령어

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# ktLint 검사
./gradlew ktlintCheck

# ktLint 자동 포맷팅
./gradlew ktlintFormat

# commerce-api 실행
./gradlew :apps:commerce-api:bootRun

# Docker 인프라 실행 (MySQL, Redis, Kafka)
docker-compose -f docker/infra-compose.yml up -d
```

## 테스트 환경

- 테스트 프로파일: `test`
- 타임존: `Asia/Seoul`
- Testcontainers를 통한 MySQL, Redis 통합 테스트 지원

## API 응답 형식

```kotlin
data class ApiResponse<T>(
    val meta: Metadata,  // result: SUCCESS/FAIL, errorCode, message
    val data: T?
)
```

## 에러 처리

```kotlin
// CoreException + ErrorType 사용
throw CoreException(ErrorType.NOT_FOUND, "커스텀 메시지")

// ErrorType 예시
enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    BAD_REQUEST, NOT_FOUND, CONFLICT, INTERNAL_ERROR
}
```

## Swagger 문서화 규칙

- **ApiSpec 인터페이스에 Swagger 어노테이션을 분리한다.** Controller는 Spring 어노테이션(`@GetMapping`, `@PathVariable` 등)만 작성한다.
- 참고 구현: `ExampleV1ApiSpec.kt`, `ExampleV1Controller.kt`

### 필수 어노테이션 체크리스트

| 위치 | 어노테이션 | 필수 |
|------|-----------|:----:|
| ApiSpec 인터페이스 | `@Tag(name, description)` | O |
| ApiSpec 각 메서드 | `@Operation(summary, description)` | O |
| ApiSpec 각 메서드 | `@ApiResponses` (가능한 응답 코드별) | O |
| ApiSpec 메서드 파라미터 | `@Parameter(description, required)` | O |
| DTO 클래스 | `@Schema(description)` | O |
| DTO 필드 | `@Schema(description, example)` | O |



## 코드 작성 원칙

### 코드 스타일 & 추상화 원칙 중요
- early return을 사용하고, 중첩 if문/else 사용을 최소화한다.
- 매직 넘버는 상수로 정의하고, 의미 없는 축약어를 사용하지 않는다.
- 하나의 메서드는 하나의 일만 하고, 코드 깊이(indent)는 2단계를 넘지 않는다.
- 하나의 메서드에는 하나의 추상화 수준만 존재해야 한다.
- 비즈니스 로직과 기술 구현 로직을 섞지 않는다.
- null 반환을 지양하고, 컬렉션은 null 대신 빈 컬렉션을 반환한다.
- 
### 필수 원칙
1. **실제 동작하는 해결책만 고려** - 이론적/가상의 코드 금지
2. **null-safety 필수** - Java의 경우 `Optional` 적극 활용
3. **thread-safety 고려** - 동시성 이슈가 발생할 수 있는 코드 주의
4. **테스트 가능한 구조로 설계** - 의존성 주입, 단일 책임 원칙 준수
5. **기존 코드 패턴 분석 후 일관성 유지** - 새 코드는 기존 스타일과 통일
### 금지 사항
- 실제 동작하지 않는 코드 작성 금지
- 불필요한 Mock 데이터를 이용한 구현 금지
- `System.out.println()` 등 디버그용 출력문 남기지 말 것
- null-safety 하지 않은 코드 작성 금지
### 일반 규칙
- 한글 주석 사용 가능
- Lombok 사용 (`@Getter`, `@Builder`, `@RequiredArgsConstructor` 등)
- 메서드/클래스에 `@Method 설명`, `@작성일`, `@작성자` 주석 필수 패턴 사용
### 네이밍 규칙
- Entity: `*Entity` (예: `SubscriptionOrderEntity`)
- DTO: `*Dto` (예: `SubscriptionOrderDto`)
- Repository: `*Repository` (예: `SubscriptionOrderRepository`)
- Service: `*Service` (예: `SubscriptionOrderService`)
### API 패턴
- 조회는 GetMapping, 생성/수정/삭제는 PostMapping을 사용한다.
- 조회: `search*`, `find*`, `get*`
- 생성: `create*`, `regist*`
- 수정: `update*`, `modify*`
- 삭제: `delete*`, `remove*`
### 코드 스타일
1. 삼항 연산자 사용을 지양한다.
2. 한 줄에는 하나의 책임만 가진다.
3. 메서드는 가능한 짧게 유지한다.
4. 중첩 if 문을 줄이고 early return을 사용한다.
5. else 사용을 최소화한다.
6. 매직 넘버를 직접 쓰지 말고 상수로 정의한다.
7. boolean 변수는 긍정형 이름을 사용한다.
8. 의미 없는 축약어를 사용하지 않는다.
9. 변수명과 메서드명은 역할이 드러나도록 작성한다.
10. 주석보다 이름으로 설명한다.
11. null 반환을 지양하고 Optional 또는 객체를 사용한다.
12. 하나의 메서드는 하나의 일만 하도록 만든다.
13. 조건문이 길어지면 의미 있는 변수로 분리한다.
14. 반복되는 코드는 반드시 메서드로 추출한다.
15. 코드 깊이(indent)는 2단계를 넘지 않도록 한다.
16. 한 메서드에서 여러 수준의 추상화를 섞지 않는다.
17. 예외는 숨기지 말고 명확하게 처리한다.
18. 컬렉션은 null 대신 빈 컬렉션을 반환한다.
19. getter/setter 남용을 지양한다.
20. 테스트 가능한 구조로 작성한다.
21. 로그는 의도를 설명하도록 작성한다.
22. 상수는 의미 있는 이름으로 선언한다.
23. switch 대신 다형성을 우선 고려한다.
24. 불필요한 public 노출을 줄인다.
25. 코드 스타일보다 가독성을 우선한다.
### 추상화 원칙
1. 하나의 메서드에는 하나의 추상화 수준만 존재해야 한다.
2. 상위 추상화 코드에서 하위 구현 세부사항을 직접 다루지 않는다.
3. 구현보다 의도를 먼저 드러내는 이름을 사용한다.
4. 메서드 이름만 보고도 내부 구현을 추측할 수 있어야 한다.
5. 추상화 레벨이 다른 로직은 반드시 메서드로 분리한다.
6. 비즈니스 로직과 기술 구현 로직을 섞지 않는다.
7. 상위 로직은 "무엇을 하는가"를 표현하고, 하위 로직은 "어떻게 하는가"를 담당한다.
8. 구현 세부사항은 가능한 가장 낮은 레벨로 숨긴다.
9. 외부에 노출되는 인터페이스는 최소한의 개념만 포함한다.
10. 추상화는 재사용보다 이해를 쉽게 만드는 것을 우선한다.
11. 읽는 사람이 구현을 따라가지 않아도 흐름을 이해할 수 있어야 한다.
12. 한 메서드 안에서 서로 다른 관심사를 처리하지 않는다.
13. 메서드 이름이 길어지는 것은 추상화가 부족하다는 신호로 본다.
14. 조건 분기가 많아지면 추상화를 다시 설계한다.
15. 구현 설명이 필요한 코드는 추상화가 잘못된 것으로 본다.
16. 계층 간 의존성은 한 방향으로만 흐르게 한다.
17. 상위 계층은 하위 계층의 내부 구조를 몰라야 한다.
18. 추상화는 숨김이 아니라 의도 표현이다.
19. 공통 로직 추출보다 책임 분리가 우선이다.
20. 추상화는 코드 재사용보다 변경에 강한 구조를 목표로 한다.


## 성능 최적화 가이드
### JPA/Hibernate
- N+1 문제가 발생할 수 있는 양방향 @OneToOne을 지양한다.
- N+1 문제는 `fetch join`, `@BatchSize`로 해결한다.
- 전체 조회를 지양하고, `Pageable`을 적용하거나 필요한 컬럼만 Projection한다.
### QueryDSL
- 전체 Entity를 조회하지 말고, 필요한 필드만 Projection하여 DTO로 반환한다.
### Stream API
- 동일한 조건으로 스트림을 여러 번 순회하지 말고, 한 번 필터링한 결과를 재사용한다.
### 성능 주의사항
- 조회 메서드에는 `@Transactional(readOnly = true)`를 적용한다.
- Lazy Loading은 트랜잭션 범위 안에서만 접근하여 `LazyInitializationException`을 방지한다.
- 자주 조회하는 컬럼에 인덱스가 존재하는지 확인한다.

## 금지 사항 (DB)
- **`ddl-auto`는 절대 `none`에서 변경하지 않는다.** (`update`, `create`, `create-drop`, `validate` 등으로 변경 금지)
- DB 스키마 변경(DDL)이 필요하면 SQL을 사용자에게 제공하고, 사용자가 직접 실행한다.
- 테스트 DB의 데이터를 임의로 변경하지 않는다. 변경이 필요하면 반드시 원복한다.

## 일반 주의사항
- 커밋 전 반드시 빌드 테스트 수행
- Entity 수정 시 관련 DTO, Repository, Service 연쇄 확인
- QueryDSL 사용 시 Q클래스 자동 생성 확인 (`./gradlew compileQuerydsl`)
- 
## 관련 프로젝트
- `store-mypage-v2`: 고객 마이페이지 API 서버
- 정기구독(subscription) 관련해서 동일한 Entity 구조를 공유하므로 로직 수정 시 양쪽 확인 필요
