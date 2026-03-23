# 회식투표 서비스 설계문서

> **작성일**: 2026-03-23
> **프로젝트**: company-service
> **상태**: 구현 완료
> **최종 수정**: 2026-03-23 (코드 리뷰 반영, 요구사항 변경 반영)

---

## 1. 개요

### 1.1 목적
팀 내부에서 공통으로 사용하는 서비스의 첫 번째 기능으로 **회식 후보지 등록 및 투표** 기능을 구현한다.

### 1.2 주요 기능
- **간편 인증**: 이름 + 숫자 4자리 비밀번호 (자동 회원가입/로그인)
- **월별 후보지 등록**: 가게이름, 음식종류, 링크, 등록자 정보
- **월별 투표**: 후보지 기반 투표 생성 (관리자 전용), 투표, 변경, 결과 확인
- **날짜 투표**: 투표 시 가능 날짜 선택 (달력 UI)

### 1.3 비기능 요구사항
- 데스크톱 우선, 모바일 반응형
- 토스 스타일 미니멀 UI
- 내부 서비스 수준의 보안 (BCrypt 비밀번호 해시)

---

## 2. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| Language | Kotlin 2.2.21, Java 21 | |
| Framework | Spring Boot 4.0.4 | Spring Web MVC |
| ORM | Spring Data JPA + Hibernate 7.x | |
| Database | MySQL 8.0 | localhost:3307, FK 미사용 |
| Template | Thymeleaf (fragment 방식) | SSR, layout-dialect 미사용 |
| Frontend | HTMX (CDN) + TailwindCSS (CDN) | 빌드 불필요 |
| Auth | HandlerInterceptor + HttpSession | Spring Security 미사용 |
| Password | BCrypt (spring-security-crypto) | 해시 저장 |
| Build | Gradle 9.4.0 (Kotlin DSL) | |
| Test | JUnit 5, Mockito-Kotlin | TDD |

---

## 3. 아키텍처

### 3.1 레이어 구조

```
interfaces/web/     → Thymeleaf Controller, DTO (Presentation)
application/        → Service (@Transactional) (Application)
domain/             → Entity, Repository Interface (Domain)
infrastructure/     → JPA Repository 구현체 (Infrastructure)
support/            → 공통 설정, 에러 처리, 인증, 유틸 (Cross-cutting)
```

### 3.2 패키지 구조

```
com.yoonbeom.companyservice/
├── interfaces/web/
│   ├── auth/          → AuthController, LoginRequest
│   ├── restaurant/    → RestaurantController, RegisterRestaurantRequest
│   ├── vote/          → VoteController (관리자 권한 체크), CreateVoteRequest
│   └── MainController
├── application/
│   ├── auth/          → AuthService
│   ├── restaurant/    → RestaurantService
│   └── vote/          → VoteService
├── domain/
│   ├── member/        → Member, MemberRepository
│   ├── restaurant/    → Restaurant, RestaurantRepository
│   └── vote/          → Vote, VoteItem, VoteBallot, VoteDateBallot + Repositories
├── infrastructure/
│   ├── member/        → MemberJpaRepository
│   ├── restaurant/    → RestaurantJpaRepository
│   └── vote/          → VoteJpaRepository (fetch join), VoteItem/Ballot JpaRepositories
└── support/
    ├── config/        → WebMvcConfig, PasswordEncoderConfig
    ├── error/         → ErrorType, CoreException, GlobalExceptionHandler
    ├── auth/          → AuthInterceptor
    └── util/          → YearMonthUtils
```

### 3.3 프로파일 구성

| 프로파일 | 용도 | DB |
|---------|------|-----|
| local | 로컬 개발 | localhost:3307/company_service |
| real | Docker 운영 | host.docker.internal:3307/company_service |
| test | 테스트 | localhost:3307/company_service_test |

---

## 4. 도메인 모델

### 4.1 ERD (FK 제약 없음, 애플리케이션 레벨 참조)

```
member ←── restaurant (registrant_id)
member ←── vote (created_by_id)
member ←── vote_ballot (voter_id)
member ←── vote_date_ballot (voter_id)
vote ←── vote_item (vote_id)
vote ←── vote_date_ballot (vote_id)
restaurant ←── vote_item (restaurant_id)
vote_item ←── vote_ballot (vote_item_id)
```

### 4.2 테이블 정의

#### member
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 사용자 이름 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| created_at | DATETIME(6) | NOT NULL | 가입일시 |

#### restaurant
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | 가게이름 |
| food_type | VARCHAR(50) | NOT NULL | 음식종류 |
| link | VARCHAR(500) | NULLABLE | 링크 |
| registrant_id | BIGINT | NOT NULL | 등록자 |
| target_month | VARCHAR(7) | NOT NULL | "YYYY-MM" |
| created_at | DATETIME(6) | NOT NULL | 등록일시 |

#### vote
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| title | VARCHAR(100) | NOT NULL | 투표 제목 |
| target_month | VARCHAR(7) | NOT NULL | "YYYY-MM" |
| max_selections | INT | NOT NULL | 최대 선택 수 |
| deadline | DATETIME(6) | NOT NULL | 마감시간 |
| created_by_id | BIGINT | NOT NULL | 생성자 (관리자만 가능) |
| created_at | DATETIME(6) | NOT NULL | 생성일시 |

#### vote_item
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| vote_id | BIGINT | NOT NULL | 소속 투표 |
| restaurant_id | BIGINT | NOT NULL | 연결 후보지 |
| | | UNIQUE(vote_id, restaurant_id) | |

#### vote_ballot
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| vote_item_id | BIGINT | NOT NULL | 투표한 항목 |
| voter_id | BIGINT | NOT NULL | 투표자 |
| created_at | DATETIME(6) | NOT NULL | 투표일시 |
| | | UNIQUE(vote_item_id, voter_id) | |

#### vote_date_ballot
| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| vote_id | BIGINT | NOT NULL | 소속 투표 |
| voter_id | BIGINT | NOT NULL | 투표자 |
| available_date | DATE | NOT NULL | 선택한 날짜 |
| created_at | DATETIME(6) | NOT NULL | 투표일시 |
| | | UNIQUE(vote_id, voter_id, available_date) | |

---

## 5. 화면 설계

### 5.1 화면 흐름

```
/login → POST /login → / (리다이렉트) → /restaurants

[탭: 후보지 | 투표]

/restaurants?yearMonth=YYYY-MM    → 월별 후보지 목록 + 등록 폼
POST /restaurants                 → HTMX 부분 갱신

/votes?yearMonth=YYYY-MM          → 투표 목록 (관리자: +투표 만들기 버튼)
POST /votes                       → 투표 생성 (관리자 전용, 모달 팝업)
/votes/{id}                       → 투표 상세 (장소 투표 + 달력 날짜 투표)
POST /votes/{id}/ballot           → 투표하기/변경
GET /votes/{id}/result-fragment   → 투표 현황 (HTMX polling 10s)
```

### 5.2 투표 만들기 (관리자 전용)
- **이름이 "관리자"인 사용자만** 투표 목록 페이지에서 `+ 투표 만들기` 버튼이 표시됨
- 버튼 클릭 시 **모달 팝업**으로 투표 생성 폼 표시
- 서버에서도 관리자 권한 검증 (비관리자 POST 시 UNAUTHORIZED 에러)

### 5.3 투표 상세 (`/votes/{id}`)

**진행중:**
- 장소 선택 (체크박스, 최대 N개)
- 가능 날짜 선택 (해당 월 달력 UI, 다중 선택)
- 투표하기 버튼
- 투표 현황 (10초 자동 갱신)

**마감됨:**
- 장소 1등 (공동 1등 포함) 강조 표시
- 전체 득표수 정렬
- 날짜별 가능 인원 수 표시

---

## 6. 인증 설계

### 6.1 로그인 로직
- 없는 이름 → 자동 회원 생성 + 로그인
- 있는 이름 + 맞는 비밀번호 → 로그인
- 있는 이름 + 틀린 비밀번호 → 에러 메시지

### 6.2 비밀번호
- 숫자 4자리 제한 (서버: `@Valid LoginRequest` + `@Pattern`, 클라이언트: `pattern` attribute)
- BCrypt 해시 저장

### 6.3 세션
- `HttpSession` (cookie 기반, `session.tracking-modes=cookie`)
- 타임아웃: 24시간

### 6.4 권한
- **관리자** (이름: "관리자"): 투표 생성 가능
- **일반 유저**: 후보지 등록, 투표 참여만 가능

---

## 7. DB 설계 원칙

- **FK 제약 없음**: 모든 테이블 간 참조는 애플리케이션 레벨에서 관리
- `ddl-auto=none` (프로덕션), `create-drop` (테스트)
- DDL 변경 시 `docs/sql/ddl.sql` 업데이트 후 사용자가 직접 실행

---

## 8. 코드 리뷰 반영 사항

| 이슈 | 해결 |
|------|------|
| LoginRequest DTO 미사용 | `@Valid LoginRequest` + `BindingResult` 적용 |
| N+1 문제 | `findVoteById` fetch join + `@BatchSize(100)` |
| unsafe cast | `as? String ?: throw CoreException` 패턴 |
| dead code | `Member.matchesPassword()` 제거 |
| 비즈니스 로직 누출 | `Vote.validateCanVote()` Entity 캡슐화 |
| 중복 코드 | `YearMonthUtils`, `YEAR_MONTH_PATTERN` 공통화 |
| 에러 페이지 없음 | `error.html` 독립 페이지 추가 |
| layout-dialect NPE | layout-dialect 제거, fragment 방식으로 변경 |

---

## 9. 실행 방법

```bash
# 로컬 개발
./gradlew bootRun --args='--spring.profiles.active=local'

# Docker 실행
docker compose up --build -d

# 테스트
./gradlew test

# DDL 실행 (최초 1회)
docker exec -i mysql-docker mysql -uroot -proot < docs/sql/ddl.sql
```
