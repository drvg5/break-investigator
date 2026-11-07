# Trade Matching Policy

## Purpose and scope
This policy sets the rules for matching our booked equity trades against counterparty confirmations in the US and
CA markets. A trade is matched when every economic field on the confirmation agrees with our booking within the
tolerances below. Settlements Operations owns matching; the trading desk owns the booking.

## Fields that must match
- Instrument (ISIN).
- Quantity: must match exactly. There is no quantity tolerance.
- Price: must match within the price tolerance below.
- Trade date and settlement date: must match exactly.
- Side: the counterparty states its own side, so our BUY must be confirmed as their SELL and our SELL as their BUY.
- Settlement details: the counterparty's custodian BIC and account must agree with a standing settlement
  instruction (SSI) we hold for them in that market.

## Price tolerance
A confirmed price may differ from our booked price by up to 10 basis points (0.10%) of our price and still count as
matched. The difference in basis points is |their price - our price| / our price x 10,000. A difference above
10 bps is a price break and must be resolved before settlement. Never widen the tolerance to clear a single trade.

## Confirmation timeline
Counterparties are expected to confirm on trade date. A trade with no confirmation by the evening matching run on
trade date is reported as UNMATCHED. Because both markets settle on T+1, an unmatched trade must be resolved early on
the morning of T+1 or it risks failing.

## Who may change what
Operations may not amend economic fields of a booking. Quantity, price and dates are amended only by the trading
desk or middle office after the trader confirms the correct terms. Operations may chase counterparties, request
re-confirmations and raise amendment requests.
