package com.loan.decision.loanintake.service;

import com.loan.decision.loanintake.controller.dto.LoanApplicationRequest;
import com.loan.decision.loanintake.controller.dto.LoanApplicationResponse;
import com.loan.decision.loanintake.model.Applicant;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.ApplicantRepository;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanIntakeService {

    private final ApplicantRepository applicantRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Transactional
    public LoanApplicationResponse submitApplication(LoanApplicationRequest request) {
        log.info("Processing loan application for email: {}", request.getEmail());

        Applicant applicant = findOrCreateApplicant(request);

        LoanApplication application = LoanApplication.builder()
                .applicant(applicant)
                .requestedAmount(request.getRequestedAmount())
                .termMonths(request.getTermMonths())
                .loanPurpose(request.getLoanPurpose())
                .monthlyIncome(request.getMonthlyIncome())
                .monthlyExpenses(request.getMonthlyExpenses())
                .status(LoanApplication.ApplicationStatus.PENDING)
                .build();

        LoanApplication saved = loanApplicationRepository.save(application);
        log.info("Loan application created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplication(UUID applicationId) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        return mapToResponse(application);
    }

    private Applicant findOrCreateApplicant(LoanApplicationRequest request) {
        return applicantRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    Applicant newApplicant = Applicant.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .phoneNumber(request.getPhoneNumber())
                            .dateOfBirth(request.getDateOfBirth())
                            .nationalId(request.getNationalId())
                            .employmentStatus(request.getEmploymentStatus())
                            .employerName(request.getEmployerName())
                            .build();
                    return applicantRepository.save(newApplicant);
                });
    }

    private LoanApplicationResponse mapToResponse(LoanApplication application) {
        return LoanApplicationResponse.builder()
                .id(application.getId())
                .applicantId(application.getApplicant().getId())
                .applicantName(application.getApplicant().getFirstName() + " " + application.getApplicant().getLastName())
                .requestedAmount(application.getRequestedAmount())
                .termMonths(application.getTermMonths())
                .loanPurpose(application.getLoanPurpose())
                .status(application.getStatus().name())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
