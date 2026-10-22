package io.breakinvestigator.bankcore.api;

import io.breakinvestigator.bankcore.domain.BreakStatus;
import io.breakinvestigator.bankcore.repo.BreakRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/breaks")
public class BreakController {

    private final BreakRepository breaks;

    public BreakController(BreakRepository breaks) {
        this.breaks = breaks;
    }

    /** The break queue. status = OPEN (default), IN_PROGRESS, RESOLVED or ALL. */
    @GetMapping
    public List<BreakSummary> list(@RequestParam(defaultValue = "OPEN") String status) {
        if (status.equalsIgnoreCase("ALL")) {
            return breaks.findByStatus(null);
        }
        return breaks.findByStatus(BreakStatus.valueOf(status.toUpperCase()).name());
    }

    @GetMapping("/{breakRef}")
    public BreakDetail get(@PathVariable String breakRef) {
        return breaks.findByRef(breakRef)
                .orElseThrow(() -> new NotFoundException("No break with ref " + breakRef));
    }
}
