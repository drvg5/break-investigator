package io.breakinvestigator.bankcore.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.breakinvestigator.bankcore.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SeedLoaderTest extends IntegrationTestSupport {

    @Autowired
    SeedLoader loader;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    @Test
    void startupLoadsTheWholeGeneratedWorld() {
        assertThat(count("trade")).isEqualTo(seed.trades().size());
        assertThat(count("confirmation")).isEqualTo(seed.confirmations().size());
        assertThat(count("settlement_instruction")).isEqualTo(seed.ssis().size());
        assertThat(count("trade_break")).isEqualTo(seed.breaks().size());
        assertThat(count("break_event")).isEqualTo(seed.breaks().size());
    }

    @Test
    void manifestIsWrittenOutsideTheDatabase() throws Exception {
        SeedManifest manifest = json.readValue(new File(props.manifestPath()), SeedManifest.class);
        assertThat(manifest.breaks()).extracting(SeedManifest.Entry::breakRef, SeedManifest.Entry::injectedType)
                .containsExactlyElementsOf(seed.manifest().breaks().stream()
                        .map(e -> tuple(e.breakRef(), e.injectedType())).toList());
    }

    @Test
    void rerunDoesNotReseedANonEmptyDatabase() throws Exception {
        loader.run(null);
        assertThat(count("trade")).isEqualTo(seed.trades().size());
    }
}
