package io.breakinvestigator.bankcore.api;

import io.breakinvestigator.bankcore.repo.ConfirmationRepository;
import io.breakinvestigator.bankcore.repo.TradeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeRepository trades;
    private final ConfirmationRepository confirmations;

    public TradeController(TradeRepository trades, ConfirmationRepository confirmations) {
        this.trades = trades;
        this.confirmations = confirmations;
    }

    @GetMapping("/{tradeRef}")
    public TradeDetail get(@PathVariable String tradeRef) {
        return trades.findByRef(tradeRef)
                .orElseThrow(() -> new NotFoundException("No trade with ref " + tradeRef));
    }

    /** Confirmations received for the trade. 404 if the trade is unknown, empty list if none received. */
    @GetMapping("/{tradeRef}/confirmations")
    public List<ConfirmationView> confirmations(@PathVariable String tradeRef) {
        if (trades.findByRef(tradeRef).isEmpty()) {
            throw new NotFoundException("No trade with ref " + tradeRef);
        }
        return confirmations.findByTradeRef(tradeRef);
    }
}
