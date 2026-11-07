"""Search against the real kb.chunks table. Skipped unless Postgres is up and `kb-ingest` has run."""

import pytest

from break_investigator_kb import store
from break_investigator_kb.embedding import Embedder
from break_investigator_kb.retrieval import RRF_K, hybrid_search, keyword_search, vector_search


@pytest.fixture(scope="module")
def conn():
    try:
        connection = store.connect()
        count = connection.execute("SELECT count(*) FROM kb.chunks").fetchone()[0]
    except Exception as e:  # no database, or never ingested
        pytest.skip(f"knowledge base not available: {e}")
    if not count:
        pytest.skip("kb.chunks is empty; run kb-ingest")
    yield connection
    connection.close()


@pytest.fixture(scope="module")
def embedder():
    return Embedder()


def ids(hits):
    return [h.chunk_id for h in hits]


def test_keyword_search_finds_exact_terms(conn):
    assert ids(keyword_search(conn, "basis points tolerance", 3))[0] == "matching-policy#price-tolerance"


def test_keyword_search_matches_any_word_not_all(conn):
    # Not every word appears in one chunk, which an all-words match would turn into no results.
    assert keyword_search(conn, "what tolerance applies to the confirmed price of a widget", 5)


def test_keyword_search_returns_nothing_for_no_match_or_no_words(conn):
    assert keyword_search(conn, "zebra", 5) == []
    assert keyword_search(conn, "?!", 5) == []


def test_vector_search_finds_paraphrases(conn, embedder):
    # Shares almost no words with the stale SSI runbook, so only meaning can find it.
    hits = vector_search(conn, embedder, "the account details we sent were out of date on value date", 5)
    assert any(h.doc_id == "runbook-stale-ssi" for h in hits)


def test_results_are_best_first_and_limited_to_k(conn, embedder):
    for hits in (keyword_search(conn, "cut-off", 4), vector_search(conn, embedder, "cut-off", 4),
                 hybrid_search(conn, embedder, "cut-off", 4)):
        assert len(hits) == 4
        assert [h.score for h in hits] == sorted((h.score for h in hits), reverse=True)


def test_hybrid_scores_are_reciprocal_rank_fusion(conn, embedder):
    hits = hybrid_search(conn, embedder, "late amendment after the cut-off", 5)
    assert hits[0].score <= 2 / (RRF_K + 1)  # first in both lists is the maximum possible
    assert len(set(ids(hits))) == len(hits)


def test_hybrid_ranks_a_chunk_found_by_both_methods_highly(conn, embedder):
    query = "which settlement instruction version applies to a trade"
    in_both = set(ids(keyword_search(conn, query, 20))) & set(ids(vector_search(conn, embedder, query, 20)))
    assert hybrid_search(conn, embedder, query, 1)[0].chunk_id in in_both
