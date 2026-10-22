package io.breakinvestigator.bankcore.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * On startup, if the trade table is empty: generate the synthetic world, insert it,
 * and write the ground-truth manifest to a file the API never serves.
 */
@Component
@ConditionalOnProperty(name = "bank.seed.enabled", havingValue = "true")
public class SeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final SeedProperties props;

    public SeedLoader(JdbcTemplate jdbc, ObjectMapper objectMapper, SeedProperties props) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Long existing = jdbc.queryForObject("select count(*) from trade", Long.class);
        if (existing != null && existing > 0) {
            log.info("Seed skipped: {} trades already present. Drop the volume to reseed.", existing);
            return;
        }

        SeedData data = new SeedGenerator(props.randomSeed(), props.businessDate(),
                props.cleanTrades(), props.breaksPerType()).generate();
        insert(data);

        Path manifest = Path.of(props.manifestPath()).toAbsolutePath().normalize();
        Files.createDirectories(manifest.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifest.toFile(), data.manifest());

        log.info("Seeded {} trades, {} confirmations, {} SSIs, {} breaks (seed={}). Manifest: {}",
                data.trades().size(), data.confirmations().size(), data.ssis().size(), data.breaks().size(),
                props.randomSeed(), manifest);
    }

    private void insert(SeedData d) {
        batch("insert into counterparty (id, code, name, lei, bic) values (?,?,?,?,?)",
                d.counterparties().stream().map(c -> new Object[]{c.id(), c.code(), c.name(), c.lei(), c.bic()}).toList());

        batch("insert into instrument (id, isin, ticker, name, market, currency, asset_class) values (?,?,?,?,?,?,?)",
                d.instruments().stream().map(i -> new Object[]{i.id(), i.isin(), i.ticker(), i.name(), i.market(),
                        ReferenceData.market(i.market()).currency(), ReferenceData.ASSET_CLASS}).toList());

        batch("""
                insert into settlement_instruction (id, counterparty_id, market, asset_class, custodian_bic, account_number,
                    place_of_settlement, cash_correspondent_bic, effective_from, effective_to)
                values (?,?,?,?,?,?,?,?,?,?)""",
                d.ssis().stream().map(s -> new Object[]{s.id(), s.counterpartyId(), s.market(), ReferenceData.ASSET_CLASS,
                        s.custodianBic(), s.accountNumber(), s.placeOfSettlement(), s.cashCorrespondentBic(),
                        s.effectiveFrom(), s.effectiveTo()}).toList());

        batch("""
                insert into trade (id, trade_ref, counterparty_id, instrument_id, side, quantity, price, gross_amount,
                    currency, trade_date, settlement_date, booked_at, last_amended_at, settlement_instruction_id, status)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                d.trades().stream().map(t -> new Object[]{t.id(), t.tradeRef(), t.counterpartyId(), t.instrumentId(),
                        t.side().name(), t.quantity(), t.price(), t.grossAmount(), t.currency(), t.tradeDate(),
                        t.settlementDate(), t.bookedAt(), t.lastAmendedAt(), t.ssiId(), t.status().name()}).toList());

        batch("""
                insert into confirmation (id, confirmation_ref, trade_ref, counterparty_id, isin, counterparty_side,
                    quantity, price, trade_date, settlement_date, custodian_bic, account_number, place_of_settlement,
                    remarks, received_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                d.confirmations().stream().map(c -> new Object[]{c.id(), c.confirmationRef(), c.tradeRef(),
                        c.counterpartyId(), c.isin(), c.counterpartySide().name(), c.quantity(), c.price(),
                        c.tradeDate(), c.settlementDate(), c.custodianBic(), c.accountNumber(),
                        c.placeOfSettlement(), c.remarks(), c.receivedAt()}).toList());

        batch("insert into trade_break (id, break_ref, trade_id, symptom, status, detected_at) values (?,?,?,?,?,?)",
                d.breaks().stream().map(b -> new Object[]{b.id(), b.breakRef(), b.tradeId(), b.symptom().name(),
                        b.status().name(), b.detectedAt()}).toList());

        batch("insert into break_event (break_id, event_type, actor, details, created_at) values (?,?,?,?,?)",
                d.breaks().stream().map(b -> new Object[]{b.id(), "CREATED", "settlement-monitor",
                        "Break raised: " + b.symptom().name(), b.detectedAt()}).toList());
    }

    private void batch(String sql, List<Object[]> rows) {
        jdbc.batchUpdate(sql, rows);
    }
}
