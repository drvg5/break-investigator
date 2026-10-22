package io.breakinvestigator.bankcore.api;

import io.breakinvestigator.bankcore.repo.SsiRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SettlementInstructionController {

    private final SsiRepository ssis;

    public SettlementInstructionController(SsiRepository ssis) {
        this.ssis = ssis;
    }

    /** Every SSI version we hold for the counterparty, including expired ones. Empty list = none on file. */
    @GetMapping("/api/counterparties/{code}/settlement-instructions")
    public List<SsiView> list(@PathVariable String code, @RequestParam(required = false) String market) {
        if (!ssis.counterpartyExists(code)) {
            throw new NotFoundException("No counterparty with code " + code);
        }
        return ssis.findByCounterparty(code, market == null ? null : market.toUpperCase());
    }
}
