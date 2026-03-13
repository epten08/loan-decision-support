package com.loan.decision.outcomes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.loanintake.model.LoanApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanOutcomeController.class)
class LoanOutcomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanOutcomeService outcomeService;

    private UUID applicationId;
    private LoanApplication application;
    private LoanOutcome outcome;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        application = new LoanApplication();
        application.setId(applicationId);

        outcome = LoanOutcome.builder()
                .id(UUID.randomUUID())
                .loanApplication(application)
                .outcome(LoanOutcome.OutcomeType.REPAID)
                .recordedAt(LocalDateTime.now())
                .recordedBy("SYSTEM")
                .build();
    }

    @Test
    @DisplayName("POST /api/loan-applications/{id}/outcome should create outcome")
    void recordOutcome_validRequest_returnsCreated() throws Exception {
        // Arrange
        when(outcomeService.recordOutcome(
                eq(applicationId),
                eq(LoanOutcome.OutcomeType.REPAID),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(outcome);

        LoanOutcomeController.OutcomeRequest request = new LoanOutcomeController.OutcomeRequest();
        request.setOutcome(LoanOutcome.OutcomeType.REPAID);

        // Act & Assert
        mockMvc.perform(post("/api/loan-applications/{id}/outcome", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("REPAID"))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()));
    }

    @Test
    @DisplayName("POST /api/loan-applications/{id}/outcome should create outcome with details")
    void recordOutcome_withDetails_returnsCreated() throws Exception {
        // Arrange
        outcome.setDaysLate(30);
        outcome.setAmountRecovered(new BigDecimal("5000.00"));
        outcome.setRecoveryStatus("IN_PROGRESS");
        outcome.setNotes("Test notes");
        outcome.setOutcome(LoanOutcome.OutcomeType.DELINQUENT);

        when(outcomeService.recordOutcome(
                eq(applicationId),
                eq(LoanOutcome.OutcomeType.DELINQUENT),
                eq(30),
                any(BigDecimal.class),
                eq("IN_PROGRESS"),
                eq("Test notes")
        )).thenReturn(outcome);

        LoanOutcomeController.OutcomeRequest request = new LoanOutcomeController.OutcomeRequest();
        request.setOutcome(LoanOutcome.OutcomeType.DELINQUENT);
        request.setDaysLate(30);
        request.setAmountRecovered(new BigDecimal("5000.00"));
        request.setRecoveryStatus("IN_PROGRESS");
        request.setNotes("Test notes");

        // Act & Assert
        mockMvc.perform(post("/api/loan-applications/{id}/outcome", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("DELINQUENT"))
                .andExpect(jsonPath("$.daysLate").value(30))
                .andExpect(jsonPath("$.recoveryStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/loan-applications/{id}/outcome should return 400 for missing outcome")
    void recordOutcome_missingOutcome_returnsBadRequest() throws Exception {
        // Arrange
        String invalidRequest = "{}";

        // Act & Assert
        mockMvc.perform(post("/api/loan-applications/{id}/outcome", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/loan-applications/{id}/outcome should return outcome")
    void getOutcome_exists_returnsOk() throws Exception {
        // Arrange
        when(outcomeService.getOutcome(applicationId)).thenReturn(outcome);

        // Act & Assert
        mockMvc.perform(get("/api/loan-applications/{id}/outcome", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REPAID"))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()));
    }

    @Test
    @DisplayName("GET /api/loan-applications/{id}/outcome should return 400 when not found")
    void getOutcome_notFound_returns400() throws Exception {
        // Arrange
        when(outcomeService.getOutcome(applicationId))
                .thenThrow(new IllegalArgumentException("Outcome not found"));

        // Act & Assert
        mockMvc.perform(get("/api/loan-applications/{id}/outcome", applicationId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/loan-applications/{id}/outcome/exists should return exists status")
    void hasOutcome_exists_returnsTrue() throws Exception {
        // Arrange
        when(outcomeService.hasOutcome(applicationId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/loan-applications/{id}/outcome/exists", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.hasOutcome").value(true));
    }

    @Test
    @DisplayName("GET /api/loan-applications/{id}/outcome/exists should return false when not exists")
    void hasOutcome_notExists_returnsFalse() throws Exception {
        // Arrange
        when(outcomeService.hasOutcome(applicationId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/loan-applications/{id}/outcome/exists", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOutcome").value(false));
    }

    @Test
    @DisplayName("PUT /api/loan-applications/{id}/outcome should update outcome")
    void updateOutcome_validRequest_returnsOk() throws Exception {
        // Arrange
        outcome.setOutcome(LoanOutcome.OutcomeType.DEFAULTED);
        outcome.setNotes("Updated notes");

        when(outcomeService.updateOutcome(
                eq(applicationId),
                eq(LoanOutcome.OutcomeType.DEFAULTED),
                eq("Updated notes")
        )).thenReturn(outcome);

        LoanOutcomeController.OutcomeUpdateRequest request = new LoanOutcomeController.OutcomeUpdateRequest();
        request.setOutcome(LoanOutcome.OutcomeType.DEFAULTED);
        request.setNotes("Updated notes");

        // Act & Assert
        mockMvc.perform(put("/api/loan-applications/{id}/outcome", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DEFAULTED"))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    @DisplayName("GET /api/loan-applications/outcomes/statistics should return statistics")
    void getStatistics_returnsStatistics() throws Exception {
        // Arrange
        LoanOutcomeService.OutcomeStatistics stats = LoanOutcomeService.OutcomeStatistics.builder()
                .totalOutcomes(100)
                .repaid(70)
                .defaulted(10)
                .delinquent(5)
                .writtenOff(5)
                .performing(8)
                .prepaid(2)
                .defaultRate(0.15)
                .build();

        when(outcomeService.getStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/loan-applications/outcomes/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutcomes").value(100))
                .andExpect(jsonPath("$.repaid").value(70))
                .andExpect(jsonPath("$.defaulted").value(10))
                .andExpect(jsonPath("$.defaultRate").value(0.15));
    }
}
