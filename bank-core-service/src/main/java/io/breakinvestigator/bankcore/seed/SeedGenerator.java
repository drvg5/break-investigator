package io.breakinvestigator.bankcore.seed;

import io.breakinvestigator.bankcore.domain.BreakStatus;
import io.breakinvestigator.bankcore.domain.Side;
import io.breakinvestigator.bankcore.domain.TradeStatus;
import io.breakinvestigator.bankcore.seed.ReferenceData.Counterparty;
import io.breakinvestigator.bankcore.seed.ReferenceData.Instrument;
import io.breakinvestigator.bankcore.seed.ReferenceData.Market;
import io.breakinvestigator.bankcore.seed.SeedData.BreakRow;
import io.breakinvestigator.bankcore.seed.SeedData.ConfirmationRow;
import io.breakinvestigator.bankcore.seed.SeedData.SsiRow;
import io.breakinvestigator.bankcore.seed.SeedData.TradeRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static io.breakinvestigator.bankcore.seed.ReferenceData.*;

/**
 * Builds the synthetic world: reference data, SSIs (with history), trades, confirmations and breaks,
 * plus the ground-truth manifest. Pure and deterministic: no DB, no clock, only {@code Random(seed)}.
 */
public final class SeedGenerator {

    private static final LocalDate SSI_EPOCH = LocalDate.of(2024, 1, 1);

    private final long seed;
    private final LocalDate businessDate;
    private final int cleanTrades;
    private final int breaksPerType;
    private final Random rnd;

    /** US SSI version changes on this date for {@link ReferenceData#US_SSI_CHANGED} counterparties. */
    private final LocalDate ssiChangeDate;

    private final List<SsiRow> ssis = new ArrayList<>();

    public SeedGenerator(long seed, LocalDate businessDate, int cleanTrades, int breaksPerType) {
        this.seed = seed;
        this.businessDate = businessDate;
        this.cleanTrades = cleanTrades;
        this.breaksPerType = breaksPerType;
        this.rnd = new Random(seed);
        this.ssiChangeDate = minusBusinessDays(businessDate, 4);
    }

    public SeedData generate() {
        buildSsis();

        // Plan which trades break and how, then shuffle so trade refs carry no signal (ADR-011).
        List<InjectedBreakType> plan = new ArrayList<>();
        for (int i = 0; i < cleanTrades; i++) plan.add(null);
        for (InjectedBreakType type : InjectedBreakType.values()) {
            for (int i = 0; i < breaksPerType; i++) plan.add(type);
        }
        Collections.shuffle(plan, rnd);

        List<TradeRow> trades = new ArrayList<>();
        List<ConfirmationRow> confirmations = new ArrayList<>();
        List<PendingBreak> pending = new ArrayList<>();

        for (int i = 0; i < plan.size(); i++) {
            InjectedBreakType type = plan.get(i);
            long id = i + 1;
            String tradeRef = "TRD-%06d".formatted(240001 + i);
            buildTrade(id, tradeRef, type, trades, confirmations, pending);
        }

        // Break refs follow detection order, so BRK-0001.. don't cluster by type.
        pending.sort(Comparator.comparing(PendingBreak::detectedAt).thenComparing(p -> p.trade().tradeRef()));
        List<BreakRow> breaks = new ArrayList<>();
        List<SeedManifest.Entry> entries = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            PendingBreak p = pending.get(i);
            String breakRef = "BRK-%04d".formatted(i + 1);
            breaks.add(new BreakRow(i + 1, breakRef, p.trade().id(), p.type().symptom(), BreakStatus.OPEN, p.detectedAt()));
            entries.add(new SeedManifest.Entry(breakRef, p.trade().tradeRef(), p.type(), p.type().symptom(),
                    p.summary(), p.evidence()));
        }

        var manifest = new SeedManifest(seed, businessDate, PRICE_TOLERANCE_BPS, trades.size(), cleanTrades, entries);
        return new SeedData(COUNTERPARTIES, INSTRUMENTS, List.copyOf(ssis), trades, confirmations, breaks, manifest);
    }

    // ---------------------------------------------------------------- SSIs

    private void buildSsis() {
        long id = 1;
        for (Counterparty c : COUNTERPARTIES) {
            for (Market m : MARKETS) {
                if (m == CA && NO_CA_SSI.contains(c.code())) continue;
                if (m == US && US_SSI_CHANGED.contains(c.code())) {
                    ssis.add(newSsi(id++, c, m, SSI_EPOCH, ssiChangeDate.minusDays(1)));
                    ssis.add(newSsi(id++, c, m, ssiChangeDate, null));
                } else {
                    ssis.add(newSsi(id++, c, m, SSI_EPOCH, null));
                }
            }
        }
    }

    private SsiRow newSsi(long id, Counterparty c, Market m, LocalDate from, LocalDate to) {
        return new SsiRow(id, c.id(), m.code(), pick(CUSTODIAN_BICS), randomAccount(), m.csdBic(),
                pick(CASH_CORRESPONDENT_BICS), from, to);
    }

    private SsiRow validSsi(long counterpartyId, String market, LocalDate date) {
        return ssis.stream()
                .filter(s -> s.counterpartyId() == counterpartyId && s.market().equals(market) && s.validOn(date))
                .findFirst().orElse(null);
    }

    private SsiRow expiredSsi(long counterpartyId, String market) {
        return ssis.stream()
                .filter(s -> s.counterpartyId() == counterpartyId && s.market().equals(market) && s.effectiveTo() != null)
                .findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- trades

    private record PendingBreak(TradeRow trade, InjectedBreakType type, OffsetDateTime detectedAt,
                                String summary, Map<String, Object> evidence) {
    }

    private void buildTrade(long id, String tradeRef, InjectedBreakType type,
                            List<TradeRow> trades, List<ConfirmationRow> confirmations, List<PendingBreak> pending) {
        Counterparty cpty;
        Instrument instr;
        if (type == InjectedBreakType.MISSING_SSI) {
            cpty = pickCounterparty(NO_CA_SSI);
            instr = pickInstrument(CA.code());
        } else if (type == InjectedBreakType.STALE_SSI) {
            cpty = pickCounterparty(US_SSI_CHANGED);
            instr = pickInstrument(US.code());
        } else {
            cpty = pick(COUNTERPARTIES);
            String marketCode = NO_CA_SSI.contains(cpty.code()) ? US.code() : pick(MARKETS).code();
            instr = pickInstrument(marketCode);
        }
        Market market = market(instr.market());

        // Breaks are recent (still open); clean trades are spread over the last two weeks.
        LocalDate tradeDate = type == null
                ? minusBusinessDays(businessDate, rnd.nextInt(10))
                : minusBusinessDays(businessDate, 1 + rnd.nextInt(2));
        LocalDate settlementDate = plusBusinessDays(tradeDate, 1);

        Side side = rnd.nextBoolean() ? Side.BUY : Side.SELL;
        BigDecimal quantity = BigDecimal.valueOf(100L * (1 + rnd.nextInt(500))).setScale(4);
        BigDecimal price = instr.basePrice()
                .multiply(BigDecimal.valueOf(1 + (rnd.nextDouble() - 0.5) * 0.04))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

        OffsetDateTime bookedAt = at(tradeDate, LocalTime.of(9, 30).plusMinutes(rnd.nextInt(390)), market);
        OffsetDateTime cutoff = at(tradeDate, market.instructionCutoff(), market);
        OffsetDateTime lastAmendedAt = null;
        if (type == InjectedBreakType.MISSED_CUTOFF) {
            lastAmendedAt = cutoff.plusMinutes(15 + rnd.nextInt(165));
        } else if (rnd.nextInt(10) == 0) {
            // Some clean amendments, all before cut-off, so "was amended" alone isn't a giveaway.
            lastAmendedAt = bookedAt.plusMinutes(5 + rnd.nextInt(60));
        }

        SsiRow ssi = switch (type) {
            case MISSING_SSI -> null;
            case STALE_SSI -> expiredSsi(cpty.id(), market.code());
            case null, default -> validSsi(cpty.id(), market.code(), settlementDate);
        };

        TradeStatus status;
        if (type == null) {
            status = settlementDate.isAfter(businessDate) ? TradeStatus.PENDING_SETTLEMENT : TradeStatus.SETTLED;
        } else {
            status = switch (type.symptom()) {
                case FAILED_TO_SETTLE -> TradeStatus.FAILED;
                case UNMATCHED, PENDING_PAST_CUTOFF -> TradeStatus.PENDING_SETTLEMENT;
            };
        }

        TradeRow trade = new TradeRow(id, tradeRef, cpty.id(), instr.id(), side, quantity, price, gross,
                market.currency(), tradeDate, settlementDate, bookedAt, lastAmendedAt,
                ssi == null ? null : ssi.id(), status);
        trades.add(trade);

        // ---- confirmation: mirrors the trade, then the injected type mutates one thing.
        BigDecimal confQty = quantity;
        BigDecimal confPrice = price;
        LocalDate confSettle = settlementDate;
        String confCustodian = ssi != null ? ssi.custodianBic() : pick(CUSTODIAN_BICS);
        String confAccount = ssi != null ? ssi.accountNumber() : randomAccount();
        Map<String, Object> evidence = new LinkedHashMap<>();
        String summary = null;

        if (type != null) {
            switch (type) {
                case QUANTITY_MISMATCH -> {
                    int delta = 100 * (1 + rnd.nextInt(10));
                    confQty = rnd.nextBoolean() || quantity.intValue() <= delta
                            ? quantity.add(BigDecimal.valueOf(delta)) : quantity.subtract(BigDecimal.valueOf(delta));
                    evidence.put("ourQuantity", quantity);
                    evidence.put("theirQuantity", confQty);
                    summary = "Counterparty confirmed a different quantity.";
                }
                case PRICE_MISMATCH -> {
                    double bps = 50 + rnd.nextInt(250); // 0.5%..3%, well over the 10 bps tolerance
                    double sign = rnd.nextBoolean() ? 1 : -1;
                    confPrice = price.multiply(BigDecimal.valueOf(1 + sign * bps / 10_000))
                            .setScale(2, RoundingMode.HALF_UP);
                    evidence.put("ourPrice", price);
                    evidence.put("theirPrice", confPrice);
                    evidence.put("diffBps", confPrice.subtract(price).abs()
                            .divide(price, 6, RoundingMode.HALF_UP).movePointRight(4).setScale(1, RoundingMode.HALF_UP));
                    summary = "Confirmed price differs beyond the %d bps tolerance.".formatted(PRICE_TOLERANCE_BPS);
                }
                case SETTLEMENT_DATE_MISMATCH -> {
                    confSettle = plusBusinessDays(settlementDate, 1);
                    evidence.put("ourSettlementDate", settlementDate);
                    evidence.put("theirSettlementDate", confSettle);
                    summary = "Counterparty confirmed T+2 settlement; we booked T+1.";
                }
                case MISSING_CONFIRMATION -> {
                    evidence.put("confirmationReceived", false);
                    summary = "No confirmation received from the counterparty.";
                }
                case SSI_MISMATCH -> {
                    String ourCustodian = confCustodian;
                    String ourAccount = confAccount;
                    confCustodian = pickOther(CUSTODIAN_BICS, ourCustodian);
                    confAccount = randomAccount();
                    evidence.put("ourCustodianBic", ourCustodian);
                    evidence.put("ourAccount", ourAccount);
                    evidence.put("theirCustodianBic", confCustodian);
                    evidence.put("theirAccount", confAccount);
                    summary = "Confirmation settles to an account that matches none of our SSIs for this counterparty.";
                }
                case MISSING_SSI -> {
                    evidence.put("market", market.code());
                    evidence.put("counterparty", cpty.code());
                    summary = "No SSI on file for this counterparty in this market; instruction could not be generated.";
                }
                case STALE_SSI -> {
                    SsiRow current = validSsi(cpty.id(), market.code(), settlementDate);
                    confCustodian = current.custodianBic();
                    confAccount = current.accountNumber();
                    evidence.put("usedSsiId", ssi.id());
                    evidence.put("usedSsiEffectiveTo", ssi.effectiveTo());
                    evidence.put("currentSsiId", current.id());
                    evidence.put("currentSsiEffectiveFrom", current.effectiveFrom());
                    summary = "Trade was enriched with an expired SSI; a newer SSI was valid on settlement date.";
                }
                case MISSED_CUTOFF -> {
                    evidence.put("cutoffLocal", market.instructionCutoff() + " " + market.zone());
                    evidence.put("lastAmendedAt", lastAmendedAt);
                    summary = "Trade was amended after the market instruction cut-off.";
                }
            }
        }

        if (type != InjectedBreakType.MISSING_CONFIRMATION) {
            OffsetDateTime receivedAt = at(tradeDate, LocalTime.of(16, 30).plusMinutes(rnd.nextInt(180)), market);
            String remarks = rnd.nextInt(5) == 0 ? pick(REMARKS) : null;
            confirmations.add(new ConfirmationRow(id, "CNF-%06d".formatted(810000 + id), tradeRef, cpty.id(),
                    instr.isin(), side.opposite(), confQty, confPrice, tradeDate, confSettle,
                    confCustodian, confAccount, market.csdBic(), remarks, receivedAt));
        }

        if (type != null) {
            OffsetDateTime detectedAt = switch (type.symptom()) {
                case UNMATCHED -> at(tradeDate, LocalTime.of(19, 45), market);
                case FAILED_TO_SETTLE -> at(settlementDate, LocalTime.of(16, 30), market);
                case PENDING_PAST_CUTOFF -> lastAmendedAt != null ? lastAmendedAt.plusMinutes(5) : cutoff.plusMinutes(1);
            };
            pending.add(new PendingBreak(trade, type, detectedAt, summary, evidence));
        }
    }

    // ---------------------------------------------------------------- helpers

    private Counterparty pickCounterparty(Set<String> codes) {
        return pick(COUNTERPARTIES.stream().filter(c -> codes.contains(c.code())).toList());
    }

    private Instrument pickInstrument(String market) {
        return pick(INSTRUMENTS.stream().filter(i -> i.market().equals(market)).toList());
    }

    private <T> T pick(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    private <T> T pickOther(List<T> list, T not) {
        return pick(list.stream().filter(x -> !x.equals(not)).toList());
    }

    private String randomAccount() {
        return "ZZ%010d".formatted(rnd.nextLong(10_000_000_000L));
    }

    private static OffsetDateTime at(LocalDate date, LocalTime time, Market market) {
        return date.atTime(time).atZone(market.zone()).toOffsetDateTime();
    }

    static LocalDate plusBusinessDays(LocalDate date, int days) {
        LocalDate d = date;
        while (days > 0) {
            d = d.plusDays(1);
            if (!isWeekend(d)) days--;
        }
        return d;
    }

    static LocalDate minusBusinessDays(LocalDate date, int days) {
        LocalDate d = date;
        while (days > 0) {
            d = d.minusDays(1);
            if (!isWeekend(d)) days--;
        }
        return d;
    }

    private static boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
