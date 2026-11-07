# Runbook: Stale SSI

## When to use this runbook
Use it when a trade was enriched with an SSI version that was no longer valid on the settlement date, while a
newer version was. Such trades typically fail to settle: we instruct the old account, the counterparty
instructs the current one.

## How to confirm the cause
Find the SSI version attached to the trade and its effective dates. Check whether the settlement date falls after
its effective-to date. Then look for the version that was valid on the settlement date. The counterparty's
confirmation usually matches that newer version, which distinguishes this from an SSI mismatch.

## Resolution steps
1. Request re-enrichment of the trade with the version valid on the settlement date. No SSI change is needed,
   because the correct version is already on file and verified.
2. Confirm the re-generated instruction matches the counterparty's confirmation.
3. If the cut-off has passed, agree a new settlement date with the counterparty.

## Escalation
Escalate to the team lead if the trade has already failed. Ask Static Data why the expired version was still
selected, since the same problem can affect other trades booked before the SSI changed.
