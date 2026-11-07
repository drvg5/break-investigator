# Runbook: Quantity Mismatch

## When to use this runbook
Use it when a counterparty confirmation shows a different number of shares from our booking. The break is
reported as UNMATCHED. There is no tolerance on quantity: any difference is a break.

## How to confirm the cause
Compare the confirmed quantity with the booked quantity. Check that the confirmation is for the same instrument,
trade date and side; a confirmation for a different trade can look like a quantity difference. Check whether the
desk booked the order in several fills that the counterparty confirmed as one block, or the reverse.

## Resolution steps
1. Ask the trading desk to confirm the executed quantity from the order record.
2. If our booking is wrong, request an amendment from the desk or middle office.
3. If the counterparty is wrong, ask them for a corrected confirmation.
4. Re-run matching once either side is corrected.
Amendments re-generate the settlement instruction, so make them before the market cut-off.

## Escalation
Escalate to the team lead if the desk and the counterparty disagree on the executed quantity, or if the
difference cannot be resolved before cut-off.
