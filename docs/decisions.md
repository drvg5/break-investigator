# Architecture Decision Records

Short entries: context, options considered, decision, why. Newest at the bottom.
Entries marked *(implementation)* were chosen by Claude as low-stakes defaults — veto any of them.

---

## ADR-001 — Bank core data model: normalized
**Module:** 1 · **Date:** 2026-10-19

**Options:**
- *Minimal* — Trade, Confirmation, SSI, Break. Counterparty/instrument as strings.
- *Normalized* — adds Counterparty, Instrument, BreakEvent.
- *Full back office* — adds positions, cash, settlement messages.

**Decision:** Normalized. Entities: `counterparty`, `instrument`, `settlement_instruction`, `trade`, `confirmation`, `trade_break`, `break_event`.

**Why:** SSIs need to be keyed by (counterparty, market, asset class) with history, which requires a real counterparty entity. BreakEvent gives a per-break timeline that the Module 5 audit trail can build on. Full back office (positions/cash) only buys "fail to deliver" breaks and would slow v1 down.

---

## ADR-002 — The API exposes the break *symptom*, never the root cause
**Module:** 1 · **Date:** 2026-10-19

**Options:** expose a `reason_code` on the break vs. expose only a symptom/status.

**Decision:** A break carries only a `symptom`: `UNMATCHED`, `FAILED_TO_SETTLE`, or `PENDING_PAST_CUTOFF`. The injected root cause lives in `data/seed_manifest.json`, written by the seed generator and read only by the eval harness. It is never in the database or the API.

**Why:** This is how real break queues look: they tell you *that* something is wrong, not *why*. Diagnosing the cause is the agent's job. If the API returned the cause, the agent could just copy it and root-cause accuracy would measure nothing. Symptoms are deliberately many-to-one (e.g. `FAILED_TO_SETTLE` can be SSI mismatch *or* stale SSI), so the agent has to gather evidence to tell them apart.

---

## ADR-003 — Confirmations: structured fields + free-text `remarks`
**Module:** 1 · **Date:** 2026-10-19

**Decision:** Structured economic and settlement fields, plus a nullable free-text `remarks`.

**Why:** Real confirmations carry free-text notes. It also gives Module 5 a natural place to plant prompt-injection payloads. Remarks are generated **independently of whether a trade has a break**, so they never leak the answer.

---

## ADR-004 — SSIs are versioned with effective dates
**Module:** 1 · **Date:** 2026-10-19

**Decision:** `settlement_instruction` is keyed by (counterparty, market, asset class) and has `effective_from` / `effective_to`. A counterparty can have several SSI versions. Each trade records which SSI it was enriched with (`settlement_instruction_id`).

**Why:** Without history there is no "stale SSI" break. Recording the SSI used on the trade lets the agent compare "what we used" with "what was valid on the settlement date".

---

## ADR-005 — Asset class: cash equities, T+1, two markets (US, CA)
**Module:** 1 · **Date:** 2026-10-19

**Decision:** Cash equities only, settlement T+1 (business days, weekends skipped, no holiday calendar yet). Two fictional markets, `US` (USD) and `CA` (CAD), each with its own time zone and instruction cut-off.

**Why:** T+1 makes cut-off pressure real while keeping scope tight. Fixed income or FX would add about 30% more modelling for little extra agent learning.

---

## ADR-006 — Postgres from day one (pgvector image) + Flyway
**Module:** 1 · **Date:** 2026-10-19

**Options:** H2 in-memory vs. Postgres.

**Decision:** Postgres 16 via Docker Compose, using the `pgvector/pgvector:pg16` image. Schema managed by Flyway migrations.

**Why:** Module 3 needs Postgres + pgvector anyway. One database engine means Docker Compose in v5 is close to free. Flyway gives versioned, reviewable schema changes.

---

## ADR-007 — Deterministic seeded data generator that emits the manifest
**Module:** 1 · **Date:** 2026-10-19

**Options:** hand-written SQL/JSON fixtures vs. a generator.

**Decision:** A Java generator driven by a fixed random seed (`bank.seed.random-seed`, default 42). It produces the same data on every run, inserts it on first startup (when the `trade` table is empty), and writes `data/seed_manifest.json`.

**Why:** Reproducible (same seed → same breaks → comparable eval runs), scalable (change counts in config), and the manifest is the raw material for the golden eval set (Module 6). The generator is pure (no DB, no clock), so it is unit-tested directly.

---

## ADR-008 — Break types supported
**Module:** 1 · **Date:** 2026-10-19

**Decision:** Eight injected break types:

| Type | Symptom | Distinguishing evidence |
|---|---|---|
| `QUANTITY_MISMATCH` | UNMATCHED | trade.quantity ≠ confirmation.quantity |
| `PRICE_MISMATCH` | UNMATCHED | price diff > tolerance (10 bps; tolerance will live in a policy doc in v2) |
| `SETTLEMENT_DATE_MISMATCH` | UNMATCHED | confirmation settles T+2, we booked T+1 |
| `MISSING_CONFIRMATION` | UNMATCHED | no confirmation received |
| `SSI_MISMATCH` | FAILED_TO_SETTLE | confirmation account matches no SSI we hold |
| `MISSING_SSI` | PENDING_PAST_CUTOFF | trade has no SSI; counterparty has none for that market |
| `STALE_SSI` | FAILED_TO_SETTLE | trade used an SSI expired before settlement; a newer one exists |
| `MISSED_CUTOFF` | PENDING_PAST_CUTOFF | trade amended after the market's cut-off |

Deferred to v3: red-herring (two discrepancies, one real) and false-positive control cases. Skipped: buy/sell direction mismatch (adds nothing over quantity mismatch).

**v1 end-to-end type:** `SSI_MISMATCH`. It uses all three read tools (trade, confirmation, SSI) and needs no RAG.

**Why:** A mix of easy (field diffs), medium (absence of evidence, policy-dependent) and hard (temporal SSI reasoning). Shared symptoms force real investigation rather than lookup.

---

## ADR-009 — *(implementation)* Spring `JdbcClient` with explicit SQL, not JPA
**Module:** 1 · **Date:** 2026-10-19

**Why:** The service is read-mostly over a fixed schema. Explicit SQL keeps every query visible, avoids lazy-loading and N+1 surprises, and keeps the code small. Easy to swap for Spring Data JPA if you'd rather show JPA.

---

## ADR-010 — *(implementation)* Confirmation records the counterparty's own side
**Module:** 1 · **Date:** 2026-10-19

**Decision:** The column is `counterparty_side`. If we BUY, a correct confirmation says `SELL`.

**Why:** That's what a real confirmation says. Naming the column explicitly avoids the agent misreading a correct opposite side as a direction break.

---

## ADR-011 — *(implementation)* Trade/break references are shuffled
**Module:** 1 · **Date:** 2026-10-19

**Decision:** Trades are shuffled before references are assigned. Break refs are assigned in `detected_at` order.

**Why:** Prevents patterns like "breaks are always the last 40 trade refs" or "BRK-0001..0005 are all quantity breaks". Neither the agent nor a reviewer can infer the answer from IDs.
