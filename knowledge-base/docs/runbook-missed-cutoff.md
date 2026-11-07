# Runbook: Missed Cut-off

## When to use this runbook
Use it when a trade's instruction reached the CSD after the market's instruction cut-off, usually because the
trade was booked or amended late. The trade is reported as PENDING_PAST_CUTOFF.

## How to confirm the cause
Convert the trade's booked and last amended timestamps to the market's local time and compare them with the
cut-off in the settlement cut-off schedule. Check that the trade had a valid SSI; if it had none, see the missing
SSI runbook instead. An amendment before the cut-off is not a problem.

## Resolution steps
1. Agree a new settlement date with the counterparty, usually the next business day.
2. Ask the desk or middle office to amend the settlement date, and ask the counterparty to re-confirm.
3. Check that the re-generated instruction is sent before the next cut-off.

## Escalation
Escalate to the team lead if the trade is large or the new date cannot be agreed. Report repeated late amendments
from the same desk, because the fix belongs upstream.
