package io.breakinvestigator.bankcore.api;

import java.time.OffsetDateTime;

public record BreakEventView(
        String eventType,
        String actor,
        String details,
        OffsetDateTime createdAt) {
}
