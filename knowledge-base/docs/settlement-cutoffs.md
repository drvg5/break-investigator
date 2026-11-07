# Settlement Cut-off Schedule

## Settlement cycle
US and CA equities settle on T+1: one business day after trade date. Weekends and market holidays are not business
days. A trade settles only if its instruction reaches the central securities depository (CSD) before the
instruction cut-off.

## Instruction cut-offs
| Market | CSD | Cut-off (local time, on trade date) | Time zone |
|---|---|---|---|
| US | CSDUZZ33 | 21:00 | America/New_York |
| CA | CSDCZZ33 | 20:00 | America/Toronto |

Instructions are generated from the booking and its SSI. Any amendment to a trade re-generates the instruction, so
the amendment time counts as the instruction time.

## Late instructions and amendments
An instruction or amendment that reaches the CSD after the cut-off is not processed for the intended settlement
date. The trade shows as pending past cut-off and will not settle on time. Check the trade's booked and last
amended timestamps against the cut-off in the market's local time zone, not in UTC.

## Daylight saving time
Cut-offs are fixed in local time. Their UTC equivalent shifts by one hour when daylight saving time starts or
ends, so always convert the timestamp to local time before comparing.
