package io.breakinvestigator.bankcore;

import io.breakinvestigator.bankcore.seed.SeedData;
import io.breakinvestigator.bankcore.seed.SeedGenerator;
import io.breakinvestigator.bankcore.seed.SeedManifest;
import io.breakinvestigator.bankcore.seed.SeedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for tests that need the real stack: the same Postgres image as docker-compose, Flyway, the seed loader
 * and the HTTP layer. Every subclass shares one container and one Spring context.
 *
 * The seed is deterministic, so tests regenerate it in memory to know what the database holds,
 * instead of hard-coding refs.
 */
@SpringBootTest(properties = "bank.seed.manifest-path=target/test-seed-manifest.json")
@AutoConfigureMockMvc
@Import(IntegrationTestSupport.Postgres.class)
public abstract class IntegrationTestSupport {

    @TestConfiguration(proxyBeanMethods = false)
    static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected SeedProperties props;

    protected SeedData seed;

    @BeforeEach
    void generateExpectedSeed() {
        seed = new SeedGenerator(props.randomSeed(), props.businessDate(), props.cleanTrades(), props.breaksPerType())
                .generate();
    }

    protected SeedManifest.Entry firstBreakOfType(String injectedType) {
        return seed.manifest().breaks().stream()
                .filter(e -> e.injectedType().name().equals(injectedType))
                .findFirst().orElseThrow();
    }

    protected SeedData.TradeRow trade(String tradeRef) {
        return seed.trades().stream().filter(t -> t.tradeRef().equals(tradeRef)).findFirst().orElseThrow();
    }
}
