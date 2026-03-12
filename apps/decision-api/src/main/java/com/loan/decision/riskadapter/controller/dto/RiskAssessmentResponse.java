package com.loan.decision.riskadapter.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("risk_band")
    private String riskBand;

    private BigDecimal confidence;

    @JsonProperty("model_version")
    private String modelVersion;
}
