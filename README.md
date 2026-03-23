# 팀 서비스 (Company Service)

팀 내부에서 사용하는 공통 서비스입니다. 현재 **회식투표** 기능을 제공합니다.

## 주요 기능

- **간편 로그인**: 이름 + 숫자 4자리 비밀번호 (처음이면 자동 계정 생성)
- **후보지 등록**: 월별 회식 후보 가게를 등록 (가게이름, 음식종류, 링크)
- **투표**: 관리자가 투표를 생성하면 팀원이 장소 + 가능 날짜를 투표
- **결과 확인**: 실시간 투표 현황, 마감 후 1등/공동 1등 표시

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Kotlin 2.2, Spring Boot 4.0, JPA + Hibernate |
| Frontend | Thymeleaf + HTMX + TailwindCSS (CDN) |
| Database | MySQL 8.0 |
| Build | Gradle 9.4 (Kotlin DSL) |

## 실행 방법

### 1. MySQL 준비

Docker로 MySQL을 실행합니다.

```bash
docker run -d --name mysql-docker \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3307:3306 \
  mysql:8.0
```

### 2. DDL 실행

```bash
docker exec -i mysql-docker mysql -uroot -proot < docs/sql/ddl.sql
```

### 3. 앱 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

http://localhost:8080 으로 접속합니다.

### Docker로 실행

```bash
docker compose up --build -d
```

## 테스트

```bash
./gradlew test
```

## 프로파일

| 프로파일 | 용도 | DB |
|---------|------|-----|
| `local` | 로컬 개발 | localhost:3307 |
| `real` | Docker 운영 | host.docker.internal:3307 |
| `test` | 테스트 | localhost:3307 (test DB) |

## 프로젝트 구조

```
src/main/kotlin/com/yoonbeom/companyservice/
├── interfaces/web/    # Controller, DTO
├── application/       # Service
├── domain/            # Entity, Repository Interface
├── infrastructure/    # JPA Repository
└── support/           # 설정, 에러처리, 인증, 유틸
```
