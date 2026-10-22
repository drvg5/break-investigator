package io.breakinvestigator.bankcore.repo;

import io.breakinvestigator.bankcore.api.TradeDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class TradeRepository {

    private final JdbcClient jdbc;
    private final SsiRepository ssis;

    public TradeRepository(JdbcClient jdbc, SsiRepository ssis) {
        this.jdbc = jdbc;
        this.ssis = ssis;
    }

    public Optional<TradeDetail> findByRef(String tradeRef) {
        return jdbc.sql("""
                        select t.trade_ref, c.code as counterparty_code, c.name as counterparty_name,
                               i.isin, i.ticker, i.name as instrument_name, i.market,
                               t.side, t.quantity, t.price, t.gross_amount, t.currency,
                               t.trade_date, t.settlement_date, t.booked_at, t.last_amended_at,
                               t.status, t.settlement_instruction_id
                        from trade t
                        join counterparty c on c.id = t.counterparty_id
                        join instrument i on i.id = t.instrument_id
                        where t.trade_ref = :ref
                        """)
                .param("ref", tradeRef)
                .query((rs, i) -> {
                    long ssiId = rs.getLong("settlement_instruction_id");
                    var ssi = rs.wasNull() ? null : ssis.findById(ssiId).orElse(null);
                    return new TradeDetail(
                            rs.getString("trade_ref"),
                            rs.getString("counterparty_code"),
                            rs.getString("counterparty_name"),
                            rs.getString("isin"),
                            rs.getString("ticker"),
                            rs.getString("instrument_name"),
                            rs.getString("market"),
                            rs.getString("side"),
                            rs.getBigDecimal("quantity"),
                            rs.getBigDecimal("price"),
                            rs.getBigDecimal("gross_amount"),
                            rs.getString("currency"),
                            rs.getObject("trade_date", LocalDate.class),
                            rs.getObject("settlement_date", LocalDate.class),
                            rs.getObject("booked_at", OffsetDateTime.class),
                            rs.getObject("last_amended_at", OffsetDateTime.class),
                            rs.getString("status"),
                            ssi);
                })
                .optional();
    }
}
