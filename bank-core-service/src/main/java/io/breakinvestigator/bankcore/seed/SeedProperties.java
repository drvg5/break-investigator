package io.breakinvestigator.bankcore.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties("bank.seed")
public record SeedProperties(
        boolean enabled,
        long randomSeed,
        LocalDate businessDate,
        int cleanTrades,
        int breaksPerType,
        String manifestPath) {
}
