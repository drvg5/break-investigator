package io.breakinvestigator.bankcore.api;

import java.time.OffsetDateTime;

public record BreakSummary(
        String breakRef,
        String tradeRef,
        String symptom,
        String status,
        OffsetDateTime detectedAt) {
}
