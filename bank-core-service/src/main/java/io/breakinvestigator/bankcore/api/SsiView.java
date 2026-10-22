package io.breakinvestigator.bankcore.api;

import java.time.LocalDate;

/** One version of a standing settlement instruction. effectiveTo null = open-ended. */
public record SsiView(
        long id,
        String counterpartyCode,
        String market,
        String assetClass,
        String custodianBic,
        String accountNumber,
        String placeOfSettlement,
        String cashCorrespondentBic,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
