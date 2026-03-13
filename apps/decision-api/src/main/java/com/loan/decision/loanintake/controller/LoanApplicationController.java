package com.loan.decision.loanintake.controller;

import com.loan.decision.evaluation.EvaluationOrchestrator;
import com.loan.decision.evaluation.EvaluationResult;
import com.loan.decision.loanintake.controller.dto.LoanApplicationRequest;
import com.loan.decision.loanintake.controller.dto.LoanApplicationResponse;
import com.loan.decision.loanintake.service.LoanIntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationController {

    private final LoanIntakeService loanIntakeService;
    private final EvaluationOrchestrator evaluationOrchestrator;

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @Valid @RequestBody LoanApplicationRequest request) {
        log.info("Received loan application request");
        LoanApplicationResponse response = loanIntakeService.submitApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable UUID id) {
        log.info("Fetching loan application: {}", id);
        LoanApplicationResponse response = loanIntakeService.getApplication(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<EvaluationResult> evaluateApplication(@PathVariable UUID id) {
        log.info("Evaluating loan application: {}", id);
        EvaluationResult result = evaluationOrchestrator.evaluate(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/decision")
    public ResponseEntity<EvaluationResult> getDecision(@PathVariable UUID id) {
        log.info("Fetching decision for application: {}", id);
        EvaluationResult result = evaluationOrchestrator.getDecision(id);
        return ResponseEntity.ok(result);
    }
}
