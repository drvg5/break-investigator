package io.breakinvestigator.bankcore.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** The counterparty's confirmation. counterpartySide is THEIR side: if we BUY, a correct confirm says SELL. */
public record ConfirmationView(
        String confirmationRef,
        String tradeRef,
        String counterpartyCode,
        String isin,
        String counterpartySide,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate,
        LocalDate settlementDate,
        String custodianBic,
        String accountNumber,
        String placeOfSettlement,
        String remarks,
        OffsetDateTime receivedAt) {
}
