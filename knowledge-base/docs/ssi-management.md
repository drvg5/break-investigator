# Standing Settlement Instruction (SSI) Management

## What an SSI is
A standing settlement instruction tells us where a counterparty settles in a market: the custodian BIC, the
safekeeping account, the place of settlement and the cash correspondent. We hold SSIs per counterparty and
market. Each trade is enriched with one SSI version when it is booked, and that version drives the instruction
sent to the CSD.

## Versions and effective dates
SSIs change over time, so we keep every version. Each version is valid from its effective-from date to its
effective-to date, both inclusive. An empty effective-to date means the version is open-ended. Versions for the
same counterparty and market must not overlap.

## Which version applies to a trade
The version that applies is the one valid on the trade's settlement date, not its trade date or booking date.
A version that had already ended before the settlement date is expired for that trade, even if it was correct
when the trade was booked.

## Changing an SSI
SSI changes are a known fraud route, so a change is never made on the strength of a confirmation, an email or a
phone call alone. The Static Data team verifies every new or changed SSI by calling back the counterparty on a
contact number already on file, and a second person approves it before it takes effect. Operations raises SSI
change requests but cannot make the change.

## Re-enriching a trade
When a trade carries the wrong SSI version, Operations requests re-enrichment with the correct version. This
re-generates the settlement instruction, which is then subject to the market cut-off.
