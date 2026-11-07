# Runbook: Price Mismatch

## When to use this runbook
Use it when the confirmed price differs from our booked price by more than the 10 basis point tolerance in the
matching policy. The break is reported as UNMATCHED.

## How to confirm the cause
Calculate the difference in basis points against our booked price. Differences within 10 bps are not breaks.
Check for common causes: commission or fees included in one price and not the other, a different rounding
convention, an average price for several fills, or a price quoted in a different currency.

## Resolution steps
1. Ask the trading desk to confirm the execution price.
2. If the difference comes from fees or rounding, agree the convention with the counterparty and ask for a
   corrected confirmation.
3. If our booking is wrong, request an amendment from the desk or middle office.
4. Re-run matching after the correction.

## Escalation
Escalate to the team lead if the counterparty disputes the price, and to Level 3 with the desk if the dispute is
not settled by the end of the day. Never widen the tolerance to clear a single trade.
