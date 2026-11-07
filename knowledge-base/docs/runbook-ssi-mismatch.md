# Runbook: SSI Mismatch

## When to use this runbook
Use it when the custodian or account on the counterparty's confirmation does not match the SSI attached to our
trade, and matches none of the SSI versions we hold for that counterparty and market. Such trades typically fail
to settle because each side instructs a different account.

## How to confirm the cause
Compare the custodian BIC and account on the confirmation with the SSI on the trade. Then compare them with every
SSI version we hold for the counterparty in that market, including expired ones. If the confirmation matches one
of our versions, the problem is which version the trade used, not a mismatch; see the stale SSI runbook.

## Resolution steps
1. Do not change our SSI to match the confirmation. A new account on a confirmation is a known fraud pattern.
2. Ask the counterparty, through a known contact, to confirm their correct settlement details.
3. If they have genuinely moved, raise an SSI change request with the Static Data team, which verifies it by
   call-back before it takes effect, then re-enrich the trade.
4. If their confirmation was wrong, ask them for a corrected confirmation.

## Escalation
Escalate to the team lead if the counterparty cannot be reached or the new details cannot be verified. Escalate
immediately if anything suggests the change request is not genuine.
