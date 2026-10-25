# Changelog

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
