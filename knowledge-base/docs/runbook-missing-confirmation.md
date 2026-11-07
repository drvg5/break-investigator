# Runbook: Missing Confirmation

## When to use this runbook
Use it when no confirmation has been received from the counterparty for a trade. The break is reported as
UNMATCHED.

## How to confirm the cause
Check that no confirmation exists for the trade, including under a different trade reference or on another
channel. Check that the trade was sent to the counterparty in the first place, and that it is booked against the
right counterparty.

## Resolution steps
1. Chase the counterparty's operations team for the confirmation, quoting our trade reference, instrument,
   quantity, price and dates.
2. If they have no record of the trade, ask the desk to confirm it was executed with them.
3. Once the confirmation arrives, re-run matching.
Chase early: with T+1 settlement, an unconfirmed trade at the end of trade date is close to failing.

## Escalation
Escalate to the team lead if there is no response by the morning of T+1, and to counterparty relations if the
same counterparty misses confirmations repeatedly.
