"""
Retrieval over kb.chunks: keyword, vector and hybrid search (ADR-026: hand-written by the project owner).

This file is the agreed interface. The functions below are yours to implement; their signatures and the Hit
type are what the agent and the retrieval eval will call, so change them only deliberately.

What's available:
- kb.chunks columns: chunk_id, doc_id, doc_title, section, ordinal, content, tsv (tsvector), embedding (vector(384)).
- store.connect() returns a psycopg connection with pgvector registered, so a Python list of floats can be
  passed as a query parameter with %s::vector.
- Embedder().embed_query(text) returns the query vector. Create one Embedder and reuse it; loading is slow.
"""

from dataclasses import dataclass

import psycopg

from .embedding import Embedder


@dataclass(frozen=True)
class Hit:
    chunk_id: str
    doc_id: str
    doc_title: str
    section: str
    content: str
    score: float  # higher is better; only comparable within one search method


def keyword_search(conn: psycopg.Connection, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks by full-text match on the tsv column, best first. Empty if nothing matches."""
    raise NotImplementedError


def vector_search(conn: psycopg.Connection, embedder: Embedder, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks by cosine similarity between the query's embedding and each chunk's, best first."""
    raise NotImplementedError


def hybrid_search(conn: psycopg.Connection, embedder: Embedder, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks combining keyword and vector results into one ranking, best first."""
    raise NotImplementedError
