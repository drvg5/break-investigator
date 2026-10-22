package io.breakinvestigator.bankcore.repo;

import io.breakinvestigator.bankcore.api.SsiView;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class SsiRepository {

    private static final String SELECT = """
            select s.id, c.code as counterparty_code, s.market, s.asset_class, s.custodian_bic,
                   s.account_number, s.place_of_settlement, s.cash_correspondent_bic,
                   s.effective_from, s.effective_to
            from settlement_instruction s
            join counterparty c on c.id = s.counterparty_id
            """;

    static final RowMapper<SsiView> MAPPER = (rs, i) -> new SsiView(
            rs.getLong("id"),
            rs.getString("counterparty_code"),
            rs.getString("market"),
            rs.getString("asset_class"),
            rs.getString("custodian_bic"),
            rs.getString("account_number"),
            rs.getString("place_of_settlement"),
            rs.getString("cash_correspondent_bic"),
            rs.getObject("effective_from", LocalDate.class),
            rs.getObject("effective_to", LocalDate.class));

    private final JdbcClient jdbc;

    public SsiRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SsiView> findById(long id) {
        return jdbc.sql(SELECT + " where s.id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    /** All versions (including expired) for a counterparty, optionally filtered by market. Newest first. */
    public List<SsiView> findByCounterparty(String counterpartyCode, String market) {
        return jdbc.sql(SELECT + """
                         where c.code = :code
                           and (cast(:market as varchar) is null or s.market = :market)
                         order by s.market, s.effective_from desc
                        """)
                .param("code", counterpartyCode)
                .param("market", market)
                .query(MAPPER)
                .list();
    }

    public boolean counterpartyExists(String counterpartyCode) {
        return jdbc.sql("select count(*) from counterparty where code = :code")
                .param("code", counterpartyCode)
                .query(Long.class)
                .single() > 0;
    }
}
