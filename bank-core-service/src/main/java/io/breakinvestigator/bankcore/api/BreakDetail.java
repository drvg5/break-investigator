package io.breakinvestigator.bankcore.api;

import java.time.OffsetDateTime;
import java.util.List;

public record BreakDetail(
        String breakRef,
        String tradeRef,
        String symptom,
        String status,
        OffsetDateTime detectedAt,
        OffsetDateTime resolvedAt,
        List<BreakEventView> events) {
}
