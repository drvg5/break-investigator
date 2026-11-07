# Changelog

## v0.4 — in progress — Module 3: knowledge base
**Changed:** 13 synthetic documents (8 runbooks, matching policy, cut-off schedule, SSI management, escalation,
triage guide), consistent with the seed data's markets, cut-offs and 10 bps tolerance. Ingestion pipeline in
`knowledge-base/`: chunk by section, embed locally with `bge-small-en-v1.5`, store in `kb.chunks` with a full-text
index and an HNSW vector index. `retrieval.py` defines the search interface, to be hand-written. ADR-022..026.

**Measured:** 13 documents give 56 chunks, 116 to 474 characters each, so the 1,200-character cap never triggers.
10 tests pass. Ingested into real Postgres; both indexes answer queries. No retrieval quality numbers yet.

## v0.3 — 2026-11-01 — Module 4: agent (v1 slice)
**Changed:** LangGraph agent (`agent/`) with fixed stages: load break, classify, gather evidence (model chooses
tools, at most 8 calls), analyse. Ends with a structured recommendation citing tool results. No write tools.
ADR-019..021.

**Measured:** root cause correct on 10 of 10 breaks with `claude-opus-5-5`, one run each, scored by hand against
the manifest: 5 `SSI_MISMATCH` and 5 `STALE_SSI`, which share a symptom. All 10 were rated high confidence. 9 unit
tests pass with a scripted model.

**Limits:** a small sample of two break types, used while building, so this is a smoke test and not an accuracy
figure. With no wrong answers it says nothing about confidence calibration, and only the root cause label was
scored, not the explanation, fix or citations. The other 6 break types are unmeasured.

**Found and fixed:** MCP tools report failures as content rather than raising, so a failed break lookup would have
been classified by the model as if it were a break record. `load_break` now checks the error status and stops.

## v0.2.1 — 2026-10-25 — Bank core test coverage
**Changed:** 17 integration tests for the bank core service (API, repositories, seed loader) running on real
Postgres via Testcontainers (ADR-017). JaCoCo added with a 95% line and branch gate on `mvn verify` (ADR-018).
Removed a dead null check in `SeedLoader`.

**Measured:** 22 Java tests pass. Coverage went from 54.6% lines / 83.7% branches (generator only) to 99.5% lines /
96.1% branches. `repo` and `api` went from 0% to 100% lines.

**Improved:** the SSI ordering that the `get_settlement_instructions` description promises (market, then latest
`effectiveFrom` first; shipped in v0.2) is now pinned by a test, so changing the query can't silently make the tool
description wrong.

## v0.2 — 2026-10-22 — Module 2: MCP server
**Changed:** Python MCP server (`mcp-server/`, official SDK v2, stdio, managed with uv) exposing five read-only
tools, one per bank API endpoint: `list_open_breaks`, `get_break`, `get_trade`, `get_confirmations`,
`get_settlement_instructions`. Descriptions explain field meanings and what empty results mean, with no
diagnostic hints. Every tool is annotated `readOnlyHint`. ADR-012..016.

**Measured:** 11 unit tests pass against a mocked bank API (tool set, read-only hints, description hygiene,
pass-through, error mapping, empty-list regression). Live check: all five tools called through a real MCP
stdio client against the running Java service.

**Found and fixed:** bare list returns produced zero MCP content blocks for an empty list, so "no confirmation"
and "no SSI on file" were invisible to clients that read only content. Lists are now wrapped in a named object.
Also: SDK v2 renamed `FastMCP` to `MCPServer`, and docstrings were sent with source indentation, now dedented.

## v0.1 — 2026-10-19 — Module 1: bank core service
**Changed:** Spring Boot 3.4 / Java 21 service on Postgres (pgvector image) with Flyway. Normalized model:
counterparty, instrument, versioned SSIs, trade, confirmation, trade_break, break_event. Five read endpoints.
Deterministic seed generator: 200 trades, 8 injected break types × 5, ground-truth manifest written to a file only.

**Measured:** 5 generator unit tests pass (determinism, counts, every injected break contains its discrepancy,
clean trades contain none, break refs don't cluster by type). Manual smoke test of all endpoints against Postgres.

**Improved:** n/a, first version.
