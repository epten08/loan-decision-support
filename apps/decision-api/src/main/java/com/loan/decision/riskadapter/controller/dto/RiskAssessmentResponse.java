package com.loan.decision.riskadapter.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentResponse {

    private BigDecimal pd;
    private String riskBand;
    private BigDecimal confidence;
    private String modelVersion;
}
