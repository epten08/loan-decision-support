package com.loan.decision.loanintake.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    private UUID id;
    private UUID applicantId;
    private String applicantName;
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private String loanPurpose;
    private String status;
    private LocalDateTime createdAt;
}
