package com.loan.decision.outcomes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for loan outcome operations.
 * Provides endpoints for recording and retrieving loan outcomes.
 */
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
@Slf4j
public class LoanOutcomeController {

    private final LoanOutcomeService outcomeService;

    /**
     * Records the outcome of a loan.
     *
     * @param applicationId the loan application ID
     * @param request the outcome request
     * @return the recorded outcome
     */
    @PostMapping("/{applicationId}/outcome")
    public ResponseEntity<OutcomeResponse> recordOutcome(
            @PathVariable UUID applicationId,
            @Valid @RequestBody OutcomeRequest request) {

        log.info("Recording outcome {} for application: {}", request.getOutcome(), applicationId);

        LoanOutcome outcome = outcomeService.recordOutcome(
                applicationId,
                request.getOutcome(),
                request.getDaysLate(),
                request.getAmountRecovered(),
                request.getRecoveryStatus(),
                request.getNotes()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(outcome));
    }

    /**
     * Gets the outcome for a loan application.
     *
     * @param applicationId the loan application ID
     * @return the outcome
     */
    @GetMapping("/{applicationId}/outcome")
    public ResponseEntity<OutcomeResponse> getOutcome(@PathVariable UUID applicationId) {
        log.info("Getting outcome for application: {}", applicationId);

        LoanOutcome outcome = outcomeService.getOutcome(applicationId);
        return ResponseEntity.ok(toResponse(outcome));
    }

    /**
     * Checks if an outcome exists for an application.
     *
     * @param applicationId the loan application ID
     * @return true if outcome exists
     */
    @GetMapping("/{applicationId}/outcome/exists")
    public ResponseEntity<OutcomeExistsResponse> hasOutcome(@PathVariable UUID applicationId) {
        boolean exists = outcomeService.hasOutcome(applicationId);
        return ResponseEntity.ok(new OutcomeExistsResponse(applicationId, exists));
    }

    /**
     * Updates an existing outcome.
     *
     * @param applicationId the loan application ID
     * @param request the update request
     * @return the updated outcome
     */
    @PutMapping("/{applicationId}/outcome")
    public ResponseEntity<OutcomeResponse> updateOutcome(
            @PathVariable UUID applicationId,
            @Valid @RequestBody OutcomeUpdateRequest request) {

        log.info("Updating outcome for application: {} to {}", applicationId, request.getOutcome());

        LoanOutcome outcome = outcomeService.updateOutcome(
                applicationId,
                request.getOutcome(),
                request.getNotes()
        );

        return ResponseEntity.ok(toResponse(outcome));
    }

    /**
     * Gets outcome statistics.
     *
     * @return outcome statistics
     */
    @GetMapping("/outcomes/statistics")
    public ResponseEntity<LoanOutcomeService.OutcomeStatistics> getStatistics() {
        log.info("Getting outcome statistics");
        return ResponseEntity.ok(outcomeService.getStatistics());
    }

    /**
     * Handles IllegalArgumentException by returning a 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles IllegalStateException by returning a 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    private OutcomeResponse toResponse(LoanOutcome outcome) {
        return OutcomeResponse.builder()
                .id(outcome.getId())
                .applicationId(outcome.getLoanApplication().getId())
                .outcome(outcome.getOutcome())
                .daysLate(outcome.getDaysLate())
                .amountRecovered(outcome.getAmountRecovered())
                .recoveryStatus(outcome.getRecoveryStatus())
                .notes(outcome.getNotes())
                .recordedAt(outcome.getRecordedAt())
                .recordedBy(outcome.getRecordedBy())
                .build();
    }

    /**
     * Request to record a loan outcome.
     */
    @Data
    public static class OutcomeRequest {
        @NotNull(message = "Outcome is required")
        private LoanOutcome.OutcomeType outcome;

        private Integer daysLate;
        private BigDecimal amountRecovered;
        private String recoveryStatus;
        private String notes;
    }

    /**
     * Request to update a loan outcome.
     */
    @Data
    public static class OutcomeUpdateRequest {
        @NotNull(message = "Outcome is required")
        private LoanOutcome.OutcomeType outcome;

        private String notes;
    }

    /**
     * Response for outcome operations.
     */
    @Data
    @lombok.Builder
    public static class OutcomeResponse {
        private UUID id;
        private UUID applicationId;
        private LoanOutcome.OutcomeType outcome;
        private Integer daysLate;
        private BigDecimal amountRecovered;
        private String recoveryStatus;
        private String notes;
        private java.time.LocalDateTime recordedAt;
        private String recordedBy;
    }

    /**
     * Response for outcome exists check.
     */
    @Data
    @lombok.AllArgsConstructor
    public static class OutcomeExistsResponse {
        private UUID applicationId;
        private boolean hasOutcome;
    }

    /**
     * Error response.
     */
    @Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }
}
