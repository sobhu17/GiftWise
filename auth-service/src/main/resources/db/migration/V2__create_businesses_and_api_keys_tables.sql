-- V2__create_businesses_and_api_keys_tables.sql
-- Creates the three remaining auth-service tables.
-- Order matters: businesses first, then business_roles (FK → businesses + roles),
-- then api_keys (FK → businesses).

-- ─────────────────────────────────────────────
-- businesses
-- Represents a gift business registered on GiftWise.
-- ─────────────────────────────────────────────
CREATE TABLE businesses (
                            id          UUID            NOT NULL DEFAULT gen_random_uuid(),
                            name        VARCHAR(255)    NOT NULL,
                            email       VARCHAR(255)    NOT NULL,
                            password    VARCHAR(255)    NOT NULL,   -- BCrypt hash, never plaintext
                            is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
                            created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                            updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

                            CONSTRAINT pk_businesses PRIMARY KEY (id),
                            CONSTRAINT uq_businesses_email UNIQUE (email)
);

-- ─────────────────────────────────────────────
-- business_roles  (M:M junction table)
-- A business can have multiple roles.
-- A role can belong to multiple businesses.
-- Composite PK — no separate surrogate id needed on a pure junction table.
-- ─────────────────────────────────────────────
CREATE TABLE business_roles (
                                business_id UUID    NOT NULL,
                                role_id     UUID    NOT NULL,

                                CONSTRAINT pk_business_roles PRIMARY KEY (business_id, role_id),
                                CONSTRAINT fk_business_roles_business FOREIGN KEY (business_id)
                                    REFERENCES businesses (id) ON DELETE CASCADE,
                                CONSTRAINT fk_business_roles_role FOREIGN KEY (role_id)
                                    REFERENCES roles (id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- api_keys
-- Each business can have multiple API keys (e.g. one per environment).
-- We never store the real key — only a SHA-256 hash and a short prefix
-- so the business can identify their keys on the dashboard.
-- ─────────────────────────────────────────────
CREATE TABLE api_keys (
                          id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                          business_id     UUID            NOT NULL,
                          key_hash        VARCHAR(255)    NOT NULL,   -- SHA-256 hash of the real key
                          key_prefix      VARCHAR(20)     NOT NULL,   -- e.g. 'gw_live_a3f' shown on dashboard
                          is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                          last_used_at    TIMESTAMP       NULL,       -- NULL until first use
                          created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

                          CONSTRAINT pk_api_keys PRIMARY KEY (id),
                          CONSTRAINT uq_api_keys_key_hash UNIQUE (key_hash),
                          CONSTRAINT fk_api_keys_business FOREIGN KEY (business_id)
                              REFERENCES businesses (id) ON DELETE CASCADE
);

-- Index on key_hash — this column is queried on every widget connection.
-- Without an index, every connection would do a full table scan.
CREATE INDEX idx_api_keys_key_hash ON api_keys (key_hash);

-- Index on business_id — used when a business views their list of API keys.
CREATE INDEX idx_api_keys_business_id ON api_keys (business_id);