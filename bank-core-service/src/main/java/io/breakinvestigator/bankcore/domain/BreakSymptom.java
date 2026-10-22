package io.breakinvestigator.bankcore.domain;

/** What the break queue shows. Deliberately says nothing about root cause (ADR-002). */
public enum BreakSymptom {
    /** Our trade and the counterparty's confirmation could not be matched. */
    UNMATCHED,
    /** Instruction was released but settlement failed at the depository. */
    FAILED_TO_SETTLE,
    /** Settlement instruction was not released before the market cut-off. */
    PENDING_PAST_CUTOFF
}
