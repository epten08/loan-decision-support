package com.loan.decision.explanation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for decision explanation endpoints.
 * Provides human-readable explanations of loan decisions.
 */
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
@Slf4j
public class ExplanationController {

    private final DecisionExplanationService explanationService;

    /**
     * Returns a complete explanation of the decision for a loan application.
     * Includes rule results, risk assessment, and policy thresholds.
     *
     * @param id the loan application ID
     * @return the decision explanation
     */
    @GetMapping("/{id}/explanation")
    public ResponseEntity<DecisionExplanation> getExplanation(@PathVariable UUID id) {
        log.info("Fetching decision explanation for application: {}", id);
        DecisionExplanation explanation = explanationService.explain(id);
        return ResponseEntity.ok(explanation);
    }

    /**
     * Returns a summary explanation without detailed rule results.
     *
     * @param id the loan application ID
     * @return the summary explanation
     */
    @GetMapping("/{id}/explanation/summary")
    public ResponseEntity<DecisionExplanation> getExplanationSummary(@PathVariable UUID id) {
        log.info("Fetching decision explanation summary for application: {}", id);
        DecisionExplanation explanation = explanationService.explainSummary(id);
        return ResponseEntity.ok(explanation);
    }
}
