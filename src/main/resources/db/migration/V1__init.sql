-- V1: accounts, platform admins, stores, store membership, invitation codes

CREATE TABLE users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NULL COMMENT 'BCrypt hash. NULL for social-only accounts',
    name       VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL COMMENT 'Not unique until SMS verification is introduced',
    status     VARCHAR(20)  NOT NULL COMMENT 'ACTIVE, WITHDRAWN',
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE platform_admins
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  NOT NULL COMMENT 'ACTIVE, DISABLED',
    last_login_at DATETIME(6)  NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_platform_admins_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stores
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    business_number VARCHAR(10)  NULL COMMENT '10 digits, no hyphens',
    address         VARCHAR(255) NULL,
    phone           VARCHAR(20)  NULL,
    status          VARCHAR(20)  NOT NULL COMMENT 'ACTIVE, CLOSED',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stores_business_number UNIQUE (business_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE store_members
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    store_id    BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    role        VARCHAR(20) NOT NULL COMMENT 'OWNER, MANAGER, EMPLOYEE',
    status      VARCHAR(20) NOT NULL COMMENT 'PENDING, ACTIVE, REJECTED, LEFT',
    approved_at DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_members_store_user UNIQUE (store_id, user_id),
    INDEX idx_store_members_user_status (user_id, status),
    INDEX idx_store_members_store_status (store_id, status),
    CONSTRAINT fk_store_members_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE store_invitations
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    store_id        BIGINT      NOT NULL,
    code            VARCHAR(16) NOT NULL,
    role            VARCHAR(20) NOT NULL COMMENT 'Role granted on join (EMPLOYEE by default)',
    max_uses        INT         NULL COMMENT 'NULL = unlimited',
    used_count      INT         NOT NULL DEFAULT 0,
    expires_at      DATETIME(6) NOT NULL,
    revoked_at      DATETIME(6) NULL,
    created_by      BIGINT      NOT NULL,
    -- store_id while not revoked, NULL once revoked.
    -- UNIQUE on this column keeps at most one non-revoked code per store
    -- (MySQL has no partial unique index; UNIQUE allows multiple NULLs).
    active_store_id BIGINT GENERATED ALWAYS AS (CASE WHEN revoked_at IS NULL THEN store_id END) STORED,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_invitations_code UNIQUE (code),
    CONSTRAINT uk_store_invitations_active_store UNIQUE (active_store_id),
    INDEX idx_store_invitations_store (store_id),
    CONSTRAINT fk_store_invitations_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_invitations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_store_invitations_max_uses CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT ck_store_invitations_used_count CHECK (used_count >= 0),
    CONSTRAINT ck_store_invitations_usage_limit CHECK (max_uses IS NULL OR used_count <= max_uses)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
