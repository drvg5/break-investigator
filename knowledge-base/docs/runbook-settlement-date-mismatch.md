# Runbook: Settlement Date Mismatch

## When to use this runbook
Use it when the counterparty confirms a different settlement date from our booking. The break is reported as
UNMATCHED. Settlement dates must match exactly.

## How to confirm the cause
Both markets settle on T+1, so a booked settlement date should be one business day after the trade date. Check
whether the counterparty used an older settlement cycle such as T+2, counted a market holiday as a business day,
or agreed a non-standard settlement date with the desk.

## Resolution steps
1. If the counterparty applied the wrong cycle or calendar, ask them for a corrected confirmation on T+1.
2. If the desk agreed a non-standard date, ask the desk to confirm it and amend our booking.
3. Re-run matching after the correction.

## Escalation
Escalate to the team lead if the counterparty insists on a date the desk did not agree, or if the same
counterparty repeatedly confirms the wrong cycle; that is a static data problem on their side.
