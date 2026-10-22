# Break Investigator

AI copilot that helps a bank operations team investigate trade settlement breaks. Portfolio project. All data is synthetic.

See `CLAUDE.md` for full context and `docs/decisions.md` for design decisions.

## Modules
| # | Module | Status |
|---|---|---|
| 1 | Bank core service (Java / Spring Boot) | ✅ v0.1 |
| 2 | MCP server (Python) | — |
| 3 | Knowledge base (RAG) | — |
| 4 | Agent (LangGraph) | — |
| 5 | Guardrails + audit | — |
| 6 | Eval harness | — |
| 7 | UI, packaging, write-up | — |

## Run the bank core service

Prereqs: Java 21, Maven, Docker.

```bash
cp .env.example .env          # optional; edit POSTGRES_PORT if 5432 is taken
docker compose up -d --wait   # Postgres 16 + pgvector
cd bank-core-service
mvn spring-boot:run           # if you changed the port: DB_URL=jdbc:postgresql://localhost:5433/bankcore mvn spring-boot:run
```

On first start it creates the schema (Flyway), seeds 200 trades / 40 breaks, and writes the ground truth to `data/seed_manifest.json` (git-ignored, eval-only).
To reseed: `docker compose down -v`, then start again.

## API (read-only for now)

| Endpoint | Returns |
|---|---|
| `GET /api/breaks?status=OPEN\|IN_PROGRESS\|RESOLVED\|ALL` | Break queue: ref, trade ref, **symptom** (never root cause) |
| `GET /api/breaks/{breakRef}` | One break with its event history |
| `GET /api/trades/{tradeRef}` | Our booked trade + the SSI it was enriched with |
| `GET /api/trades/{tradeRef}/confirmations` | Counterparty confirmations (empty list = none received) |
| `GET /api/counterparties/{code}/settlement-instructions?market=US` | All SSI versions, including expired |

Tests: `cd bank-core-service && mvn test`
