package io.breakinvestigator.bankcore.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Our booked trade, with the SSI it was enriched with (null if none was found). */
public record TradeDetail(
        String tradeRef,
        String counterpartyCode,
        String counterpartyName,
        String isin,
        String ticker,
        String instrumentName,
        String market,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal grossAmount,
        String currency,
        LocalDate tradeDate,
        LocalDate settlementDate,
        OffsetDateTime bookedAt,
        OffsetDateTime lastAmendedAt,
        String status,
        SsiView settlementInstruction) {
}
