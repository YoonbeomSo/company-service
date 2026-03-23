# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# 로컬 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 실행
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "com.yoonbeom.companyservice.SomeTestClass"

# 단일 테스트 메서드
./gradlew test --tests "com.yoonbeom.companyservice.SomeTestClass.methodName"

# Docker 배포
docker compose up --build -d

# DDL 실행 (최초 1회)
docker exec -i mysql-docker mysql -uroot -proot < docs/sql/ddl.sql
```

## Tech Stack

- **Language:** Kotlin 2.2.21 on Java 21
- **Framework:** Spring Boot 4.0.4 (Spring Web MVC)
- **Template:** Thymeleaf (fragment 방식, layout-dialect 미사용) + HTMX + TailwindCSS (CDN)
- **ORM:** Spring Data JPA with Hibernate 7.x
- **Auth:** HandlerInterceptor + HttpSession (Spring Security 미사용, BCrypt만 사용)
- **Database:** MySQL 8.0 (FK 제약 없음, 애플리케이션 레벨 참조)
- **Build:** Gradle 9.4.0 (Kotlin DSL)
- **Testing:** JUnit 5, Mockito-Kotlin

## Architecture

레이어드 아키텍처 (DIP 준수) under `com.yoonbeom.companyservice`:

```
interfaces/web/     → Thymeleaf Controller, DTO (Presentation)
application/        → Service (@Transactional), Facade (2+ 서비스 조합 시)
domain/             → Entity, Repository Interface (핵심 도메인, 프레임워크 비의존)
infrastructure/     → JPA Repository 구현체
support/            → 공통 설정, 에러 처리, 인증, 유틸
```

### 현재 도메인 구조

- **member**: 회원 (이름 + 비밀번호 인증)
- **restaurant**: 월별 회식 후보지
- **vote**: 투표 (Vote, VoteItem, VoteBallot, VoteDateBallot)

### 프로파일

| 프로파일 | 용도 | DB |
|---------|------|-----|
| `local` | 로컬 개발 | localhost:3307/company_service |
| `real` | Docker 운영 | host.docker.internal:3307/company_service |
| `test` | 테스트 | localhost:3307/company_service_test (ddl-auto=create-drop) |

### 권한 체계

- **관리자** (이름: "관리자"): 투표 생성/종료 가능
- **일반 유저**: 후보지 등록, 투표 참여만 가능
- `VoteController.ADMIN_NAME` 상수로 관리

### Thymeleaf 패턴

- `layout/default.html`: 공통 head + header fragment (`th:replace`)
- 각 페이지에서 `th:replace="~{layout/default :: head('제목')}"`, `th:replace="~{layout/default :: header}"` 사용
- HTMX 부분 렌더링: `fragments/*.html` 반환
- Controller에서 `activeTab` model attribute로 탭 하이라이트

### JPA 설정

- Entity는 `var` 프로퍼티 사용 (`allopen` 플러그인 설정됨)
- Kotlin compiler: `-Xjsr305=strict`, `-Xannotation-default-target=param-property`
- N+1 방지: fetch join (`VoteJpaRepository.findVoteById`) + `@BatchSize(size = 100)`
- Lazy 컬렉션은 `@Transactional` 내에서 명시적 초기화 (`vote.voteItems.forEach { it.ballots.size }`)

## 코드 패턴

```kotlin
// Controller — Thymeleaf 뷰 반환, session에서 memberName 추출
@Controller
class XxxController(private val xxxService: XxxService) {
    val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
        ?: throw CoreException(ErrorType.UNAUTHORIZED)
}

// Service — @Transactional, 조회는 readOnly=true
@Component
class XxxService(private val xxxRepository: XxxRepository)

// Entity — 팩토리 메서드 + 비즈니스 로직 캡슐화
class Vote {
    fun validateCanVote(selectedCount: Int) { ... }
    fun getWinners(): List<VoteItem> { ... }
    companion object { fun create(...): Vote { ... } }
}

// Repository Interface (domain) → JPA 구현체 (infrastructure)
interface VoteRepository { fun findVoteById(id: Long): Vote? }
interface VoteJpaRepository : JpaRepository<Vote, Long>, VoteRepository
```

## 에러 처리

```kotlin
throw CoreException(ErrorType.NOT_FOUND, "커스텀 메시지")

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, CONFLICT, INTERNAL_ERROR,
    PASSWORD_MISMATCH, VOTE_EXPIRED, VOTE_LIMIT_EXCEEDED
}
```

`GlobalExceptionHandler`가 `CoreException`을 catch하여 `error.html` (독립 페이지) 렌더링.

## 도메인 & 객체 설계 전략

- 도메인 객체는 비즈니스 규칙을 캡슐화해야 합니다.
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공해야 합니다.
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높습니다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행합니다.

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
    - **통합 테스트**: `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`로 Service → Repository 통합 검증
    - **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`으로 HTTP 요청/응답 검증

## 주의사항

### Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현을 하지 말 것
- null-safety 하지 않게 코드 작성하지 말 것 (Kotlin의 null-safety 활용)
- `println` 코드 남기지 말 것
- 테스트를 임의로 삭제하거나 `@Disabled` 처리하지 말 것
- 요청하지 않은 기능을 임의로 구현하지 말 것
- **DB 초기화(DROP/TRUNCATE)를 임의로 수행하지 말 것** — 반드시 개발자에게 확인

### git
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

## 코드 작성 원칙

### 코드 스타일
- early return을 사용하고, 중첩 if문/else 사용을 최소화한다.
- 매직 넘버는 상수로 정의하고, 의미 없는 축약어를 사용하지 않는다.
- 하나의 메서드는 하나의 일만 하고, 코드 깊이(indent)는 2단계를 넘지 않는다.
- 하나의 메서드에는 하나의 추상화 수준만 존재해야 한다.
- 비즈니스 로직과 기술 구현 로직을 섞지 않는다.
- null 반환을 지양하고, 컬렉션은 null 대신 빈 컬렉션을 반환한다.

### API 패턴
- 조회는 GetMapping, 생성/수정/삭제는 PostMapping을 사용한다.
- 조회: `search*`, `find*`, `get*`
- 생성: `create*`, `regist*`
- 수정: `update*`, `modify*`
- 삭제: `delete*`, `remove*`

## 성능 최적화 가이드

### JPA/Hibernate
- N+1 문제는 `fetch join`, `@BatchSize`로 해결한다.
- 조회 메서드에는 `@Transactional(readOnly = true)`를 적용한다.
- Lazy Loading은 트랜잭션 범위 안에서만 접근하여 `LazyInitializationException`을 방지한다.

## 금지 사항 (DB)
- **`ddl-auto`는 프로덕션에서 절대 `none`에서 변경하지 않는다.** (테스트 프로파일은 `create-drop` 허용)
- **DB 초기화(DROP DATABASE, TRUNCATE)를 임의로 수행하지 않는다.** 개발자의 명시적 요청이 있을 때만.
- DB 스키마 변경(DDL)이 필요하면 `docs/sql/ddl.sql` 업데이트 후 사용자에게 제공한다.
- **FK 제약을 사용하지 않는다.** 모든 테이블 간 참조는 애플리케이션 레벨에서 관리한다.
