# Runbook: Missing SSI

## When to use this runbook
Use it when a trade has no SSI attached, so no settlement instruction could be generated. The trade is reported
as PENDING_PAST_CUTOFF because nothing reached the CSD in time.

## How to confirm the cause
Check that the trade carries no SSI. Then check whether we hold any SSI for the counterparty in the trade's
market. Counterparties often have an SSI for one market and not another, so check the trade's market
specifically.

## Resolution steps
1. Ask the counterparty, through a known contact, for their settlement details in that market.
2. Raise a new SSI request with the Static Data team, which verifies it by call-back before it takes effect.
3. Re-enrich the trade with the new SSI so the instruction is generated.
4. If the cut-off has already passed, agree a new settlement date with the counterparty.

## Escalation
Escalate to the team lead if the SSI cannot be set up before the next cut-off. Ask Static Data to check whether
other trades with the same counterparty and market are affected.
