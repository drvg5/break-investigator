package io.breakinvestigator.bankcore.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.breakinvestigator.bankcore.IntegrationTestSupport;
import io.breakinvestigator.bankcore.seed.SeedData.SsiRow;
import io.breakinvestigator.bankcore.seed.SeedData.TradeRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementInstructionApiTest extends IntegrationTestSupport {

    @Autowired
    ObjectMapper json;

    /** A counterparty with an expired SSI version, so there is more than one version to order. */
    private TradeRow staleSsiTrade() {
        return trade(firstBreakOfType("STALE_SSI").tradeRef());
    }

    private String counterpartyCode(TradeRow t) throws Exception {
        String body = mvc.perform(get("/api/trades/{ref}", t.tradeRef())).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("counterpartyCode").asText();
    }

    private List<SsiView> ssis(String path, String market) throws Exception {
        var request = get(path);
        if (market != null) {
            request.param("market", market);
        }
        String body = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return List.of(json.readValue(body, SsiView[].class));
    }

    @Test
    void returnsEveryVersionSortedByMarketThenLatestEffectiveFromFirst() throws Exception {
        TradeRow t = staleSsiTrade();
        List<SsiView> result = ssis("/api/counterparties/" + counterpartyCode(t) + "/settlement-instructions", null);

        // The MCP tool description promises this order (ADR-014), so it is pinned here.
        assertThat(result).extracting(SsiView::id).containsExactlyInAnyOrderElementsOf(
                seed.ssis().stream().filter(s -> s.counterpartyId() == t.counterpartyId()).map(SsiRow::id).toList());
        assertThat(result).hasSizeGreaterThan(1).isSortedAccordingTo(Comparator.comparing(SsiView::market)
                .thenComparing(SsiView::effectiveFrom, Comparator.reverseOrder()));
    }

    @Test
    void marketFilterIsCaseInsensitive() throws Exception {
        TradeRow t = staleSsiTrade();
        String market = seed.ssis().stream().filter(s -> s.id() == t.ssiId()).findFirst().orElseThrow().market();
        String path = "/api/counterparties/" + counterpartyCode(t) + "/settlement-instructions";
        assertThat(ssis(path, market.toLowerCase())).isNotEmpty().allSatisfy(s -> assertThat(s.market()).isEqualTo(market));
    }

    @Test
    void unknownCounterpartyIsNotFound() throws Exception {
        mvc.perform(get("/api/counterparties/NOSUCHCP/settlement-instructions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No counterparty with code NOSUCHCP"));
    }
}
