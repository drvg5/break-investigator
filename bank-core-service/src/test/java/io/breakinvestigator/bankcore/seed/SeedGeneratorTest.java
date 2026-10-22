package io.breakinvestigator.bankcore.seed;

import io.breakinvestigator.bankcore.seed.SeedData.ConfirmationRow;
import io.breakinvestigator.bankcore.seed.SeedData.SsiRow;
import io.breakinvestigator.bankcore.seed.SeedData.TradeRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that every injected break actually contains the discrepancy it claims, and clean trades contain none. */
class SeedGeneratorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 16);

    private final SeedData data = new SeedGenerator(42, BUSINESS_DATE, 160, 5).generate();
    private final Map<String, TradeRow> trades = data.trades().stream()
            .collect(Collectors.toMap(TradeRow::tradeRef, Function.identity()));
    private final Map<String, ConfirmationRow> confirms = data.confirmations().stream()
            .collect(Collectors.toMap(ConfirmationRow::tradeRef, Function.identity()));
    private final Map<Long, SsiRow> ssis = data.ssis().stream()
            .collect(Collectors.toMap(SsiRow::id, Function.identity()));

    @Test
    void sameSeedProducesIdenticalData() {
        assertThat(new SeedGenerator(42, BUSINESS_DATE, 160, 5).generate()).isEqualTo(data);
    }

    @Test
    void producesExpectedCounts() {
        assertThat(data.trades()).hasSize(160 + 8 * 5);
        assertThat(data.breaks()).hasSize(8 * 5);
        assertThat(data.manifest().breaks()).hasSize(8 * 5);
        assertThat(data.confirmations()).hasSize(data.trades().size() - 5); // 5 missing confirmations
    }

    @Test
    void cleanTradesHaveNoDiscrepancy() {
        Set<String> breakTrades = data.manifest().breaks().stream()
                .map(SeedManifest.Entry::tradeRef).collect(Collectors.toSet());
        data.trades().stream().filter(t -> !breakTrades.contains(t.tradeRef())).forEach(t -> {
            ConfirmationRow c = confirms.get(t.tradeRef());
            SsiRow ssi = ssis.get(t.ssiId());
            assertThat(c).as(t.tradeRef()).isNotNull();
            assertThat(c.quantity()).isEqualByComparingTo(t.quantity());
            assertThat(c.price()).isEqualByComparingTo(t.price());
            assertThat(c.settlementDate()).isEqualTo(t.settlementDate());
            assertThat(c.counterpartySide()).isEqualTo(t.side().opposite());
            assertThat(ssi).isNotNull();
            assertThat(ssi.validOn(t.settlementDate())).isTrue();
            assertThat(c.accountNumber()).isEqualTo(ssi.accountNumber());
        });
    }

    @Test
    void eachInjectedBreakContainsItsDiscrepancy() {
        for (SeedManifest.Entry e : data.manifest().breaks()) {
            TradeRow t = trades.get(e.tradeRef());
            ConfirmationRow c = confirms.get(e.tradeRef());
            SsiRow ssi = t.ssiId() == null ? null : ssis.get(t.ssiId());
            String why = e.breakRef() + " " + e.injectedType();
            switch (e.injectedType()) {
                case QUANTITY_MISMATCH -> assertThat(c.quantity()).as(why).isNotEqualByComparingTo(t.quantity());
                case PRICE_MISMATCH -> assertThat(c.price()).as(why).isNotEqualByComparingTo(t.price());
                case SETTLEMENT_DATE_MISMATCH -> assertThat(c.settlementDate()).as(why).isAfter(t.settlementDate());
                case MISSING_CONFIRMATION -> assertThat(c).as(why).isNull();
                case SSI_MISMATCH -> {
                    assertThat(ssi.validOn(t.settlementDate())).as(why).isTrue();
                    assertThat(data.ssis()).as(why).noneMatch(s -> s.accountNumber().equals(c.accountNumber()));
                }
                case MISSING_SSI -> {
                    assertThat(ssi).as(why).isNull();
                    String market = data.instruments().stream()
                            .filter(i -> i.id() == t.instrumentId()).findFirst().orElseThrow().market();
                    assertThat(data.ssis()).as(why)
                            .noneMatch(s -> s.counterpartyId() == t.counterpartyId() && s.market().equals(market));
                }
                case STALE_SSI -> {
                    assertThat(ssi.validOn(t.settlementDate())).as(why).isFalse();
                    assertThat(data.ssis()).as(why).anyMatch(s -> s.counterpartyId() == t.counterpartyId()
                            && s.validOn(t.settlementDate()) && s.accountNumber().equals(c.accountNumber()));
                }
                case MISSED_CUTOFF -> assertThat(t.lastAmendedAt()).as(why).isNotNull();
            }
        }
    }

    @Test
    void breakRefsDoNotClusterByType() {
        var firstFive = data.manifest().breaks().subList(0, 5).stream()
                .map(SeedManifest.Entry::injectedType).collect(Collectors.toSet());
        assertThat(firstFive).hasSizeGreaterThan(1);
    }
}
