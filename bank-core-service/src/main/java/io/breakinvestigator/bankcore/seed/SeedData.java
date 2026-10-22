package io.breakinvestigator.bankcore.seed;

import io.breakinvestigator.bankcore.domain.BreakStatus;
import io.breakinvestigator.bankcore.domain.BreakSymptom;
import io.breakinvestigator.bankcore.domain.Side;
import io.breakinvestigator.bankcore.domain.TradeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Everything the generator produces, as plain rows with pre-assigned ids. */
public record SeedData(
        List<ReferenceData.Counterparty> counterparties,
        List<ReferenceData.Instrument> instruments,
        List<SsiRow> ssis,
        List<TradeRow> trades,
        List<ConfirmationRow> confirmations,
        List<BreakRow> breaks,
        SeedManifest manifest) {

    public record SsiRow(long id, long counterpartyId, String market, String custodianBic, String accountNumber,
                         String placeOfSettlement, String cashCorrespondentBic,
                         LocalDate effectiveFrom, LocalDate effectiveTo) {

        boolean validOn(LocalDate date) {
            return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
        }
    }

    public record TradeRow(long id, String tradeRef, long counterpartyId, long instrumentId, Side side,
                           BigDecimal quantity, BigDecimal price, BigDecimal grossAmount, String currency,
                           LocalDate tradeDate, LocalDate settlementDate, OffsetDateTime bookedAt,
                           OffsetDateTime lastAmendedAt, Long ssiId, TradeStatus status) {
    }

    public record ConfirmationRow(long id, String confirmationRef, String tradeRef, long counterpartyId,
                                  String isin, Side counterpartySide, BigDecimal quantity, BigDecimal price,
                                  LocalDate tradeDate, LocalDate settlementDate, String custodianBic,
                                  String accountNumber, String placeOfSettlement, String remarks,
                                  OffsetDateTime receivedAt) {
    }

    public record BreakRow(long id, String breakRef, long tradeId, BreakSymptom symptom, BreakStatus status,
                           OffsetDateTime detectedAt) {
    }
}
