package io.breakinvestigator.bankcore.repo;

import io.breakinvestigator.bankcore.api.BreakDetail;
import io.breakinvestigator.bankcore.api.BreakEventView;
import io.breakinvestigator.bankcore.api.BreakSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BreakRepository {

    private static final String SELECT = """
            select b.id, b.break_ref, t.trade_ref, b.symptom, b.status, b.detected_at, b.resolved_at
            from trade_break b
            join trade t on t.id = b.trade_id
            """;

    private final JdbcClient jdbc;

    public BreakRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** status null = all statuses. Oldest first, like a work queue. */
    public List<BreakSummary> findByStatus(String status) {
        return jdbc.sql(SELECT + """
                         where (cast(:status as varchar) is null or b.status = :status)
                         order by b.detected_at, b.break_ref
                        """)
                .param("status", status)
                .query((rs, i) -> new BreakSummary(
                        rs.getString("break_ref"),
                        rs.getString("trade_ref"),
                        rs.getString("symptom"),
                        rs.getString("status"),
                        rs.getObject("detected_at", OffsetDateTime.class)))
                .list();
    }

    public Optional<BreakDetail> findByRef(String breakRef) {
        return jdbc.sql(SELECT + " where b.break_ref = :ref")
                .param("ref", breakRef)
                .query((rs, i) -> new BreakDetail(
                        rs.getString("break_ref"),
                        rs.getString("trade_ref"),
                        rs.getString("symptom"),
                        rs.getString("status"),
                        rs.getObject("detected_at", OffsetDateTime.class),
                        rs.getObject("resolved_at", OffsetDateTime.class),
                        findEvents(rs.getLong("id"))))
                .optional();
    }

    private List<BreakEventView> findEvents(long breakId) {
        return jdbc.sql("""
                        select event_type, actor, details, created_at
                        from break_event where break_id = :id order by created_at, id
                        """)
                .param("id", breakId)
                .query((rs, i) -> new BreakEventView(
                        rs.getString("event_type"),
                        rs.getString("actor"),
                        rs.getString("details"),
                        rs.getObject("created_at", OffsetDateTime.class)))
                .list();
    }
}
