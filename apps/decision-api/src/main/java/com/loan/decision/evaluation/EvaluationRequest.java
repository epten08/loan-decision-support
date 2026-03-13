package com.loan.decision.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request object for loan application evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {

    private UUID applicationId;

    /**
     * Optional flag to force re-evaluation even if decision exists.
     */
    private boolean forceReevaluation;

    public static EvaluationRequest of(UUID applicationId) {
        return EvaluationRequest.builder()
                .applicationId(applicationId)
                .forceReevaluation(false)
                .build();
    }
}
