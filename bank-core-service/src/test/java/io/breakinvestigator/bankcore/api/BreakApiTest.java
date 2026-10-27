package io.breakinvestigator.bankcore.api;

import io.breakinvestigator.bankcore.IntegrationTestSupport;
import io.breakinvestigator.bankcore.seed.SeedData.BreakRow;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreakApiTest extends IntegrationTestSupport {

    @Test
    void queueDefaultsToOpenBreaksOldestFirst() throws Exception {
        mvc.perform(get("/api/breaks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(seed.breaks().size()))
                .andExpect(jsonPath("$[*].status", everyItem(is("OPEN"))))
                .andExpect(jsonPath("$[0].breakRef").value("BRK-0001"));
    }

    @Test
    void statusFilterIsCaseInsensitiveAndAcceptsAll() throws Exception {
        mvc.perform(get("/api/breaks").param("status", "all"))
                .andExpect(jsonPath("$.length()").value(seed.breaks().size()));
        mvc.perform(get("/api/breaks").param("status", "resolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownStatusIsBadRequest() throws Exception {
        mvc.perform(get("/api/breaks").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void breakDetailCarriesSymptomAndEventHistory() throws Exception {
        BreakRow b = seed.breaks().getFirst();
        mvc.perform(get("/api/breaks/{ref}", b.breakRef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptom").value(b.symptom().name()))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$.events[0].actor").value("settlement-monitor"));
    }

    @Test
    void unknownBreakIsNotFound() throws Exception {
        mvc.perform(get("/api/breaks/BRK-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No break with ref BRK-9999"));
    }
}
