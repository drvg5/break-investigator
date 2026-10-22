package io.breakinvestigator.bankcore.seed;

import io.breakinvestigator.bankcore.domain.BreakSymptom;

import static io.breakinvestigator.bankcore.domain.BreakSymptom.*;

/**
 * Ground-truth root causes. Lives only in the seed package and the manifest file,
 * never in the database or the API (ADR-002). Symptoms are many-to-one on purpose.
 */
public enum InjectedBreakType {
    QUANTITY_MISMATCH(UNMATCHED),
    PRICE_MISMATCH(UNMATCHED),
    SETTLEMENT_DATE_MISMATCH(UNMATCHED),
    MISSING_CONFIRMATION(UNMATCHED),
    SSI_MISMATCH(FAILED_TO_SETTLE),
    MISSING_SSI(PENDING_PAST_CUTOFF),
    STALE_SSI(FAILED_TO_SETTLE),
    MISSED_CUTOFF(PENDING_PAST_CUTOFF);

    private final BreakSymptom symptom;

    InjectedBreakType(BreakSymptom symptom) {
        this.symptom = symptom;
    }

    public BreakSymptom symptom() {
        return symptom;
    }
}
