# Changelog

## v0.1 — 2026-10-19 — Module 1: bank core service
**Changed:** Spring Boot 3.4 / Java 21 service on Postgres (pgvector image) with Flyway. Normalized model:
counterparty, instrument, versioned SSIs, trade, confirmation, trade_break, break_event. Five read endpoints.
Deterministic seed generator: 200 trades, 8 injected break types × 5, ground-truth manifest written to a file only.

**Measured:** 5 generator unit tests pass (determinism, counts, every injected break contains its discrepancy,
clean trades contain none, break refs don't cluster by type). Manual smoke test of all endpoints against Postgres.

**Improved:** n/a, first version.
