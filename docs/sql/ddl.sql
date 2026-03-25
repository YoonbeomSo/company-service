-- 회식투표 서비스 DDL
-- 실행 대상: localhost:3307 MySQL

CREATE DATABASE IF NOT EXISTS company_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE company_service;

-- 회원
CREATE TABLE IF NOT EXISTS member (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    created_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회식 후보지
CREATE TABLE IF NOT EXISTS restaurant (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    food_type       VARCHAR(50)     NOT NULL,
    link            VARCHAR(500)    NULL,
    registrant_id   BIGINT          NOT NULL,
    target_month    VARCHAR(7)      NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_restaurant_target_month (target_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 투표
CREATE TABLE IF NOT EXISTS vote (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(100)    NOT NULL,
    target_month    VARCHAR(7)      NOT NULL,
    max_selections  INT             NOT NULL,
    deadline            DATETIME(6)     NOT NULL,
    include_date_vote   TINYINT(1)      NOT NULL DEFAULT 1,
    created_by_id       BIGINT          NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_vote_target_month (target_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 투표 항목 (투표 생성 시 후보지 스냅샷)
CREATE TABLE IF NOT EXISTS vote_item (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    vote_id         BIGINT  NOT NULL,
    restaurant_id   BIGINT  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_item (vote_id, restaurant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 투표 기록
CREATE TABLE IF NOT EXISTS vote_ballot (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    vote_item_id    BIGINT      NOT NULL,
    voter_id        BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_ballot (vote_item_id, voter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 날짜 투표 기록
CREATE TABLE IF NOT EXISTS vote_date_ballot (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    vote_id         BIGINT      NOT NULL,
    voter_id        BIGINT      NOT NULL,
    available_date  DATE        NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_date_ballot (vote_id, voter_id, available_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
