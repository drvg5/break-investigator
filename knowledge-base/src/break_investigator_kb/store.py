"""
Stores chunks in Postgres + pgvector, in a `kb` schema beside the bank's tables in the same database.

The schema is created here rather than by the Java service's Flyway migrations: the knowledge base is not part of
the bank's system of record, and keeping it separate means either side can be rebuilt without the other.
"""

import os

import psycopg
from pgvector.psycopg import register_vector

from .chunking import Chunk
from .embedding import DIMENSIONS

SCHEMA = f"""
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS kb;
CREATE TABLE IF NOT EXISTS kb.chunks (
    chunk_id   text PRIMARY KEY,
    doc_id     text NOT NULL,
    doc_title  text NOT NULL,
    section    text NOT NULL,
    ordinal    integer NOT NULL,
    content    text NOT NULL,
    -- for keyword search: title and section are included and weighted higher than the body
    tsv        tsvector GENERATED ALWAYS AS (
                   setweight(to_tsvector('english', doc_title || ' ' || section), 'A') ||
                   setweight(to_tsvector('english', content), 'B')) STORED,
    embedding  vector({DIMENSIONS}) NOT NULL
);
CREATE INDEX IF NOT EXISTS chunks_tsv_idx ON kb.chunks USING gin (tsv);
CREATE INDEX IF NOT EXISTS chunks_embedding_idx ON kb.chunks USING hnsw (embedding vector_cosine_ops);
"""


def database_url() -> str:
    """From the same variables as docker-compose, with the same defaults."""
    env = os.environ.get
    return (f"postgresql://{env('POSTGRES_USER', 'bankcore')}:{env('POSTGRES_PASSWORD', 'bankcore')}"
            f"@{env('POSTGRES_HOST', 'localhost')}:{env('POSTGRES_PORT', '5432')}/{env('POSTGRES_DB', 'bankcore')}")


def connect() -> psycopg.Connection:
    conn = psycopg.connect(database_url())
    conn.execute("CREATE EXTENSION IF NOT EXISTS vector")  # register_vector needs the type to exist
    conn.commit()
    register_vector(conn)
    return conn


def create_schema(conn: psycopg.Connection) -> None:
    conn.execute(SCHEMA)
    conn.commit()


def replace_all(conn: psycopg.Connection, chunks: list[Chunk], embeddings: list[list[float]]) -> None:
    """Rebuilds the whole table in one transaction, so readers never see a half-loaded knowledge base."""
    with conn.transaction():
        conn.execute("DELETE FROM kb.chunks")
        with conn.cursor() as cur:
            cur.executemany(
                "INSERT INTO kb.chunks (chunk_id, doc_id, doc_title, section, ordinal, content, embedding)"
                " VALUES (%s, %s, %s, %s, %s, %s, %s::vector)",
                [(c.chunk_id, c.doc_id, c.doc_title, c.section, c.ordinal, c.content, e)
                 for c, e in zip(chunks, embeddings, strict=True)],
            )
