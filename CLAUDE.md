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
./gradlew test --tests "com.yoonbeom.companymeal.SomeTestClass"

# Run a single test method
./gradlew test --tests "com.yoonbeom.companymeal.SomeTestClass.methodName"

# Clean build
./gradlew clean build
```

## Tech Stack

- **Language:** Kotlin 2.2.21 on Java 21
- **Framework:** Spring Boot 4.0.4 (Spring Web MVC)
- **ORM:** Spring Data JPA with Hibernate
- **Database:** MySQL
- **Build:** Gradle 9.4.0 (Kotlin DSL)
- **Testing:** JUnit 5 with Spring Boot Test

## Architecture

Standard Spring Boot layered architecture under `com.yoonbeom.companymeal`:

- **Base package:** `src/main/kotlin/com/yoonbeom/companymeal/`
- **Config:** `src/main/resources/application.properties`

JPA entities must use `var` properties (not `val`) — the `allopen` plugin is configured for `@Entity`, `@MappedSuperclass`, and `@Embeddable`.

Kotlin compiler uses `-Xjsr305=strict` for null-safety interop with Spring annotations.
