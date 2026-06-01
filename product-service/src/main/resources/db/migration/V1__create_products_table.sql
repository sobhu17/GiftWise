-- V1__create_products_table.sql
-- Core product catalog table for GiftWise.
-- Each product belongs to a business and holds the structured fields
-- used for filtering (category, age_group, occasion, price) plus a
-- 1536-dimension vector embedding for semantic similarity search.
-- The pgvector extension must be enabled before the vector column can be used.

-- pgvector ships as a PostgreSQL extension, not a built-in type.
-- This is idempotent — safe to run even if the extension already exists.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE products (
                          id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                          name            VARCHAR(255)    NOT NULL,
                          description     TEXT            NOT NULL,
                          business_id     UUID            NOT NULL,
                          price           NUMERIC         NOT NULL,
                          category        VARCHAR(20)     NOT NULL,
                          age_group       VARCHAR(20)     NOT NULL,
                          occasion        VARCHAR(20)     NOT NULL,
                          image_url       VARCHAR(2048)   NOT NULL,
                          -- Nullable: embedding is populated asynchronously after insert
                          -- by calling the embedding API (OpenAI text-embedding-3-small).
                          -- 1536 dimensions matches that model's fixed output size.
                          embedding       vector(1536),
                          is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

                          CONSTRAINT pk_product PRIMARY KEY (id),
                          CONSTRAINT fk_product_business FOREIGN KEY (business_id)
                              REFERENCES businesses (id) ON DELETE CASCADE
);

-- Supports listing/filtering all products for a given business.
CREATE INDEX idx_product_business_id ON products (business_id);

-- HNSW index for approximate nearest-neighbor search on the embedding column.
-- HNSW (Hierarchical Navigable Small World) trades a small amount of recall
-- for much faster search compared to an exact scan — essential at catalog scale.
-- vector_cosine_ops: we measure similarity by cosine distance, which is standard
-- for text embeddings (direction matters, not magnitude).
CREATE INDEX idx_product_embedding ON products
    USING hnsw (embedding vector_cosine_ops);