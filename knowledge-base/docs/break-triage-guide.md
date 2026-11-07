# Break Triage Guide

## How breaks are reported
Monitoring reports what it observed, not why. Each break carries one symptom: UNMATCHED, FAILED_TO_SETTLE or
PENDING_PAST_CUTOFF. Several root causes can produce the same symptom, so the symptom narrows the search but does
not answer it.

## Unmatched trades
An unmatched trade means our booking and the counterparty's confirmation do not agree, or there is no
confirmation. Start by checking whether a confirmation was received. If one was, compare each economic field
against the matching policy: quantity, price within tolerance, settlement date, and side.

## Failed settlements
A matched trade that fails to settle usually has a problem with where it settles. Compare the SSI attached to the
trade with the custodian and account on the counterparty's confirmation, and check whether the attached SSI
version was valid on the settlement date.

## Pending past cut-off
A trade pending past cut-off had no valid instruction at the CSD in time. Check whether an SSI was attached at all,
and whether the trade was booked or amended after the market's instruction cut-off in local time.

## Recording the investigation
Note every check made and its result, including the checks that ruled a cause out. A reviewer must be able to see
why the chosen cause is right and why the alternatives were rejected.
