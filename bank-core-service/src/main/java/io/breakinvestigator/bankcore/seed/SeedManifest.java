package io.breakinvestigator.bankcore.seed;

import io.breakinvestigator.bankcore.domain.BreakSymptom;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Ground truth for the eval harness. Written to a file, never to the DB or the API. */
public record SeedManifest(
        long randomSeed,
        LocalDate businessDate,
        int priceToleranceBps,
        int totalTrades,
        int cleanTrades,
        List<Entry> breaks) {

    public record Entry(
            String breakRef,
            String tradeRef,
            InjectedBreakType injectedType,
            BreakSymptom symptom,
            String summary,
            Map<String, Object> evidence) {
    }
}
