package io.breakinvestigator.bankcore.seed;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Fixed, fictional reference data. All names and identifiers are invented:
 * BICs and ISINs use the unassigned "ZZ" country code so they can't collide with real ones.
 */
final class ReferenceData {

    private ReferenceData() {
    }

    record Market(String code, String currency, ZoneId zone, LocalTime instructionCutoff, String csdBic) {
    }

    record Counterparty(long id, String code, String name, String lei, String bic) {
    }

    record Instrument(long id, String isin, String ticker, String name, String market, BigDecimal basePrice) {
    }

    static final String ASSET_CLASS = "EQUITY";

    /** Price tolerance before a difference counts as a break. Will be mirrored in the v2 matching policy doc. */
    static final int PRICE_TOLERANCE_BPS = 10;

    static final Market US = new Market("US", "USD", ZoneId.of("America/New_York"), LocalTime.of(21, 0), "CSDUZZ33");
    static final Market CA = new Market("CA", "CAD", ZoneId.of("America/Toronto"), LocalTime.of(20, 0), "CSDCZZ33");
    static final List<Market> MARKETS = List.of(US, CA);

    static Market market(String code) {
        return MARKETS.stream().filter(m -> m.code().equals(code)).findFirst().orElseThrow();
    }

    static final List<Counterparty> COUNTERPARTIES = List.of(
            new Counterparty(1, "HARBORPT", "Harbor Point Capital", "ZZ00HARBORPT00000001", "HRBPZZ2N"),
            new Counterparty(2, "BLUESTN", "Bluestone Markets", "ZZ00BLUESTN000000002", "BLSTZZ2N"),
            new Counterparty(3, "CEDARRDG", "Cedar Ridge Brokerage", "ZZ00CEDARRDG00000003", "CDRGZZ2T"),
            new Counterparty(4, "AURORACL", "Aurora Clearing", "ZZ00AURORACL00000004", "AURCZZ2T"),
            new Counterparty(5, "GRANITEB", "Granite Bay Partners", "ZZ00GRANITEB00000005", "GRNBZZ2N"),
            new Counterparty(6, "SILVRLN", "Silverline Trading", "ZZ00SILVRLN000000006", "SLVLZZ2N"),
            new Counterparty(7, "MAPLECR", "Maple Crest Securities", "ZZ00MAPLECR000000007", "MPLCZZ2T"),
            new Counterparty(8, "KESTREL", "Kestrel Asset Management", "ZZ00KESTREL000000008", "KSTLZZ2N"));

    /** Counterparties with no CA SSI on file. Used for MISSING_SSI breaks; clean trades avoid CA with them. */
    static final Set<String> NO_CA_SSI = Set.of("SILVRLN", "KESTREL");

    /** Counterparties whose US SSI changed recently (old version expired). Used for STALE_SSI breaks. */
    static final Set<String> US_SSI_CHANGED = Set.of("BLUESTN", "AURORACL", "GRANITEB");

    static final List<Instrument> INSTRUMENTS = List.of(
            new Instrument(1, "ZZ0000000017", "ACMX", "Acme Industrial Holdings", "US", new BigDecimal("142.50")),
            new Instrument(2, "ZZ0000000025", "NVLT", "Novalight Semiconductor", "US", new BigDecimal("318.20")),
            new Instrument(3, "ZZ0000000033", "PRWD", "Prairie Wind Energy", "US", new BigDecimal("47.85")),
            new Instrument(4, "ZZ0000000041", "HLXB", "Helix Biotherapeutics", "US", new BigDecimal("88.10")),
            new Instrument(5, "ZZ0000000058", "TRLN", "Trailhead Logistics", "US", new BigDecimal("23.40")),
            new Instrument(6, "ZZ0000000066", "OKFN", "Oakfield Financial Group", "US", new BigDecimal("61.75")),
            new Instrument(7, "ZZ0000000074", "NRMN", "Northern Mining Corp", "CA", new BigDecimal("34.60")),
            new Instrument(8, "ZZ0000000082", "LKRL", "Lakeshore Rail", "CA", new BigDecimal("156.30")),
            new Instrument(9, "ZZ0000000090", "BRCH", "Birchwood Telecom", "CA", new BigDecimal("19.95")),
            new Instrument(10, "ZZ0000000108", "FRSP", "Frost Peak Pipelines", "CA", new BigDecimal("52.10")),
            new Instrument(11, "ZZ0000000116", "MRDN", "Meridian Insurance", "CA", new BigDecimal("74.80")),
            new Instrument(12, "ZZ0000000124", "CSCD", "Cascade Retail", "CA", new BigDecimal("12.35")));

    static final List<String> CUSTODIAN_BICS = List.of(
            "CUSTZZ2AXXX", "VLTCZZ2NXXX", "ANCHZZ2TXXX", "TRSTZZ2NXXX", "KEEPZZ2TXXX", "SAFEZZ2NXXX");

    static final List<String> CASH_CORRESPONDENT_BICS = List.of("CASHZZ2NXXX", "PAYSZZ2TXXX");

    /** Benign free-text remarks, attached at random regardless of whether the trade breaks (ADR-003). */
    static final List<String> REMARKS = List.of(
            "Allocation 1 of 1.",
            "Please confirm at your earliest convenience.",
            "Block trade, single allocation.",
            "Executed on behalf of discretionary account.",
            "Contact our settlements desk with any queries.",
            "Agency execution.");
}
