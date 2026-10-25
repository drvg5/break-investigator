# Break Investigator — Project Context

This file gives Claude Code the full context for this project. Read it at the start of every session.

## Who I am and why this project exists

- Full-stack Java engineer, 10 years of experience; the last 3–4 years as a consultant embedded with banks (Morgan Stanley in Montreal, now RBC in Toronto), working directly with traders, operations teams and other stakeholders.
- Master's in Software Engineering (Concordia), plus BCA and MCA.
- Transitioning into AI engineering, targeting **Forward Deployed Engineer (FDE)** roles, with financial services as my niche.
- About two months into an AI course: comfortable with Python, LangChain, LangGraph, tool calling and agents; currently learning MCP; RAG is the main gap and will be learned inside this project.
- This project is my portfolio piece. I must be able to defend every design decision in an interview.

## How we work together

- **Claude writes most of the code**, module by module, fast. I have a full-time hybrid job, so momentum matters. Ship ugly first, then improve.
- **I make the decisions.** Before building each module, Claude lays out the real options in a few lines (with tradeoffs) and I choose. Note each decision and its reason in `docs/decisions.md` (short ADR-style entries) so I can explain them later.
- **Explain at the design level, not line by line.** For each module, explain which classes and functions exist, what each one is responsible for, and how they interact with the rest of the system. I'll ask for code-level detail when I want it.
- **I run it, break it and debug it.** When something fails, give me a moment to diagnose before handing me the fix. Debugging stories are interview material.
- **I hand-write the eval harness** (and possibly the retrieval logic). For these, Claude reviews, suggests and points out issues, but doesn't write them for me unless I ask.
- Keep a short changelog per version (what changed, what was measured, what improved).
- **I run the terminal work myself, to build FDE command-line fluency.** For anything to start, check, inspect, debug or commit (docker, mvn, curl, jq, psql, git, logs, networking), Claude gives me the commands to run, one per block, and breaks down every part: the program, each flag, each argument, pipes. Claude also says how to read the output. Claude runs commands itself only to verify its own build work, or when I ask.

## The use case

An AI copilot that helps a bank's operations team **investigate trade settlement breaks**: trades that fail to match or settle (quantity or price mismatch, wrong or missing settlement instructions, missed cut-off, missing counterparty confirmation, and so on).

Today an ops analyst does this by hand: look up the trade, compare it with the counterparty confirmation, check the settlement instructions, read the procedure, decide on a fix, and write it up. The copilot does the investigation and proposes a root cause and fix with citations; a human approves any action.

## Architecture

```
[ Ops analyst UI ]
        │
[ Agent (LangGraph) ] ── retrieves ──► [ Knowledge base: runbooks, policies (RAG) ]
        │
     MCP client
        │
[ MCP server (Python) ] ── REST ──► [ Bank core service (Java / Spring Boot) ]
        │                                   trades, confirmations, SSIs, break queue
[ Audit log + guardrails ]

[ Eval harness ] runs the whole system against a golden set of breaks
```

## Modules

1. **Bank core service (Java / Spring Boot).** A realistic fake backend: trades, counterparty confirmations, standing settlement instructions (SSIs), and a break queue. Seeded with synthetic data containing deliberately injected, known break types. Represents the "existing enterprise system" an FDE integrates with.
   - My decisions: data model; which break types to support.
2. **MCP server (Python).** Exposes the Java API as tools: `get_trade`, `list_open_breaks`, `get_confirmation`, `get_settlement_instructions`, plus one write tool, `propose_resolution`, which requires human approval. Read and write tools are kept clearly separate.
   - My decisions: tool granularity; tool names and descriptions so the model picks correctly.
3. **Knowledge base (RAG).** Synthetic ops runbooks, settlement policies, cut-off schedules, escalation procedures. Pipeline: ingest → chunk → embed → store (Postgres + pgvector) → hybrid retrieval (keyword + vector) → rerank → answer with citations.
   - My decisions: chunking strategy, embedding model, reranking, how to measure retrieval quality.
4. **Agent (LangGraph).** For a given break: classify it, gather evidence via tools, retrieve the relevant procedure, produce a root cause and proposed fix with citations, and pause for human approval before any write action.
   - My decisions: free-form agent vs. fixed workflow with LLM steps, and how much autonomy is appropriate for a bank.
5. **Guardrails and audit trail.** Log every tool call and model decision; mask sensitive fields; defend against prompt injection hidden in documents or confirmation text; enforce read/write permissions.
6. **Eval harness (I write this).** Golden set of 30–50 breaks with known root causes. Metrics: root-cause accuracy, tool-call correctness, retrieval recall@k, citation faithfulness, plus an LLM-as-judge calibrated against my own labels. Re-run on every significant change.
7. **UI, packaging, write-up.** Simple UI, Docker Compose for one-command startup, README (architecture, decisions, eval results), and a 3-minute demo video.

## Build order

- **v1 (about 2 weeks):** Java service + MCP server with three read tools + an agent that handles one break type end to end. No RAG. Working beats pretty.
- **v2:** Knowledge base and RAG; start building the golden eval set.
- **v3:** Full eval harness with real numbers; improve against them.
- **v4:** Guardrails, approval flow, audit trail.
- **v5:** UI, Docker, README, demo video.

Every version should produce a measurable "here's what I changed and what improved" story.

## Hard rules

- **Synthetic data only.** Never use documents, code, data, internal procedures or system names from RBC, Morgan Stanley or any other employer or client, not even for realism. Use general, public domain knowledge about trade settlement only.
- No real secrets in the repo: API keys go in a local `.env` file that is git-ignored; commit a `.env.example` instead.
- Keep the project runnable from a clean clone with documented steps.

## Current status

- [x] Module 1: Bank core service v0.1 (decisions ADR-001..011; see docs/CHANGELOG.md)
- [x] Module 2: MCP server v0.2 (decisions ADR-012..016)
- [ ] Module 4 (v1 slice): agent handling one break type end to end (next up: free-form agent vs. fixed workflow, autonomy)
