-- V1__create_roles_table.sql
-- Roles are seeded here and never change at runtime.
-- GiftWise has exactly two roles: BUSINESS_OWNER and ADMIN.
-- We create the table and immediately insert the seed data
-- in the same migration so the app never starts with an empty roles table.

CREATE TABLE roles (
                       id          UUID            NOT NULL DEFAULT gen_random_uuid(),
                       name        VARCHAR(50)     NOT NULL,
                       created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

                       CONSTRAINT pk_roles PRIMARY KEY (id),
                       CONSTRAINT uq_roles_name UNIQUE (name)
);

-- Seed the two fixed roles.
-- We hardcode the UUIDs so they are stable across every environment
-- (dev, staging, prod). If we let gen_random_uuid() run here, each
-- environment would have different role UUIDs which breaks any code
-- that references them by ID.
INSERT INTO roles (id, name) VALUES
                                 ('a1b2c3d4-0001-0001-0001-000000000001', 'BUSINESS_OWNER'),
                                 ('a1b2c3d4-0001-0001-0001-000000000002', 'ADMIN');