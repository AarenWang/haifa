CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS ecommerce;

CREATE TABLE IF NOT EXISTS ecommerce.product_embeddings (
    id TEXT PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(3072)
);

CREATE INDEX IF NOT EXISTS product_embeddings_vector_idx
    ON ecommerce.product_embeddings USING hnsw (embedding vector_cosine_ops);
