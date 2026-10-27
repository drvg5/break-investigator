package io.breakinvestigator.bankcore.api;

import io.breakinvestigator.bankcore.IntegrationTestSupport;
import io.breakinvestigator.bankcore.seed.SeedData.TradeRow;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeApiTest extends IntegrationTestSupport {

    @Test
    void tradeComesWithTheSsiItWasEnrichedWith() throws Exception {
        TradeRow t = trade(firstBreakOfType("STALE_SSI").tradeRef());
        mvc.perform(get("/api/trades/{ref}", t.tradeRef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.side").value(t.side().name()))
                .andExpect(jsonPath("$.settlementDate").value(t.settlementDate().toString()))
                .andExpect(jsonPath("$.settlementInstruction.id").value(t.ssiId()));
    }

    @Test
    void tradeWithoutSsiShowsNull() throws Exception {
        String ref = firstBreakOfType("MISSING_SSI").tradeRef();
        mvc.perform(get("/api/trades/{ref}", ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementInstruction").isEmpty());
    }

    @Test
    void unknownTradeIsNotFound() throws Exception {
        mvc.perform(get("/api/trades/TRD-000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No trade with ref TRD-000000"));
    }

    @Test
    void confirmationIsReturnedForAConfirmedTrade() throws Exception {
        String ref = firstBreakOfType("QUANTITY_MISMATCH").tradeRef();
        var expected = seed.confirmations().stream().filter(c -> c.tradeRef().equals(ref)).findFirst().orElseThrow();
        mvc.perform(get("/api/trades/{ref}/confirmations", ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].confirmationRef").value(expected.confirmationRef()))
                .andExpect(jsonPath("$[0].counterpartySide").value(expected.counterpartySide().name()));
    }

    @Test
    void missingConfirmationIsAnEmptyListNotAnError() throws Exception {
        String ref = firstBreakOfType("MISSING_CONFIRMATION").tradeRef();
        mvc.perform(get("/api/trades/{ref}/confirmations", ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void confirmationsOfUnknownTradeAreNotFound() throws Exception {
        mvc.perform(get("/api/trades/TRD-000000/confirmations"))
                .andExpect(status().isNotFound());
    }
}
