"""
Retrieval over kb.chunks: keyword, vector and hybrid search (ADR-026, ADR-027).

The function signatures and the Hit type are the interface the agent and the retrieval eval call.
"""

import re
from dataclasses import dataclass

import psycopg

from .embedding import Embedder

RRF_K = 60  # the standard Reciprocal Rank Fusion constant: damps the advantage of the very top ranks
CANDIDATES_PER_METHOD = 20  # each method's shortlist before fusion; deeper than k so fusion has room to reorder

_COLUMNS = "chunk_id, doc_id, doc_title, section, content"


@dataclass(frozen=True)
class Hit:
    chunk_id: str
    doc_id: str
    doc_title: str
    section: str
    content: str
    score: float  # higher is better; only comparable within one search method


def keyword_search(conn: psycopg.Connection, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks by full-text match on the tsv column, best first. Empty if nothing matches.

    Any query word may match. Requiring all of them, as Postgres does by default, would make most natural
    questions match nothing; ts_rank still ranks chunks that match more words, and title words, higher.
    """
    words = re.findall(r"\w+", query)
    if not words:
        return []
    rows = conn.execute(
        f"""SELECT {_COLUMNS}, ts_rank(tsv, q) AS score
            FROM kb.chunks, websearch_to_tsquery('english', %s) AS q
            WHERE tsv @@ q
            ORDER BY score DESC, chunk_id
            LIMIT %s""",
        (" or ".join(words), k),
    ).fetchall()
    return [Hit(*row) for row in rows]


def vector_search(conn: psycopg.Connection, embedder: Embedder, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks by cosine similarity between the query's embedding and each chunk's, best first."""
    rows = conn.execute(
        f"""SELECT {_COLUMNS}, 1 - (embedding <=> %s::vector) AS score
            FROM kb.chunks
            ORDER BY embedding <=> %s::vector, chunk_id
            LIMIT %s""",
        (vector := embedder.embed_query(query), vector, k),
    ).fetchall()
    return [Hit(*row) for row in rows]


def hybrid_search(conn: psycopg.Connection, embedder: Embedder, query: str, k: int = 5) -> list[Hit]:
    """Top k chunks combining keyword and vector results into one ranking, best first.

    Reciprocal Rank Fusion: each chunk scores the sum of 1 / (RRF_K + rank) over the lists it appears in. It uses
    ranks, not raw scores, because ts_rank and cosine similarity are on different scales that can't be compared.
    """
    rankings = [
        keyword_search(conn, query, CANDIDATES_PER_METHOD),
        vector_search(conn, embedder, query, CANDIDATES_PER_METHOD),
    ]
    fused: dict[str, float] = {}
    hits: dict[str, Hit] = {}
    for ranking in rankings:
        for rank, hit in enumerate(ranking, start=1):
            fused[hit.chunk_id] = fused.get(hit.chunk_id, 0.0) + 1 / (RRF_K + rank)
            hits.setdefault(hit.chunk_id, hit)
    best = sorted(fused, key=lambda chunk_id: (-fused[chunk_id], chunk_id))[:k]
    return [Hit(**{**hits[c].__dict__, "score": fused[c]}) for c in best]
