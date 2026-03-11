package com.loan.decision.decisioning.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResponse {

    private UUID applicationId;
    private String outcome;
    private List<String> reasonCodes;
    private String riskBand;
    private Integer hardRuleFailures;
    private Integer softRuleFailures;
    private String summary;
    private LocalDateTime decidedAt;
}
