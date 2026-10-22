package io.breakinvestigator.bankcore.repo;

import io.breakinvestigator.bankcore.api.ConfirmationView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ConfirmationRepository {

    private final JdbcClient jdbc;

    public ConfirmationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Confirmations quoting our trade ref. Usually 0 or 1; a list because the counterparty may send duplicates. */
    public List<ConfirmationView> findByTradeRef(String tradeRef) {
        return jdbc.sql("""
                        select f.confirmation_ref, f.trade_ref, c.code as counterparty_code, f.isin,
                               f.counterparty_side, f.quantity, f.price, f.trade_date, f.settlement_date,
                               f.custodian_bic, f.account_number, f.place_of_settlement, f.remarks, f.received_at
                        from confirmation f
                        join counterparty c on c.id = f.counterparty_id
                        where f.trade_ref = :ref
                        order by f.received_at
                        """)
                .param("ref", tradeRef)
                .query((rs, i) -> new ConfirmationView(
                        rs.getString("confirmation_ref"),
                        rs.getString("trade_ref"),
                        rs.getString("counterparty_code"),
                        rs.getString("isin"),
                        rs.getString("counterparty_side"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getObject("trade_date", LocalDate.class),
                        rs.getObject("settlement_date", LocalDate.class),
                        rs.getString("custodian_bic"),
                        rs.getString("account_number"),
                        rs.getString("place_of_settlement"),
                        rs.getString("remarks"),
                        rs.getObject("received_at", OffsetDateTime.class)))
                .list();
    }
}
