package com.loan.decision.riskadapter.service;

import com.loan.decision.riskadapter.controller.dto.RiskAssessmentRequest;
import com.loan.decision.riskadapter.controller.dto.RiskAssessmentResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineClientTest {

    private MockWebServer mockWebServer;
    private RiskEngineClient riskEngineClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        riskEngineClient = new RiskEngineClient(baseUrl, 5000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void assessRiskBlocking_successfulResponse_returnsRiskAssessment() throws InterruptedException {
        // Arrange
        String responseJson = """
            {
                "pd": 0.08,
                "risk_band": "B",
                "confidence": 0.82,
                "model_version": "heuristic-v1"
            }
            """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .monthlyIncome(BigDecimal.valueOf(5000))
                .requestedAmount(BigDecimal.valueOf(10000))
                .termMonths(24)
                .creditScore(700)
                .debtToIncomeRatio(BigDecimal.valueOf(0.25))
                .existingLoanCount(1)
                .build();

        // Act
        RiskAssessmentResponse response = riskEngineClient.assessRiskBlocking(request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("0.08"), response.getPd());
        assertEquals("B", response.getRiskBand());
        assertEquals(new BigDecimal("0.82"), response.getConfidence());
        assertEquals("heuristic-v1", response.getModelVersion());

        // Verify request was sent correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/risk/assess", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());

        // Verify snake_case in request body
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("\"monthly_income\""));
        assertTrue(requestBody.contains("\"requested_amount\""));
        assertTrue(requestBody.contains("\"credit_score\""));
    }

    @Test
    void assessRiskBlocking_highRiskResponse_returnsCorrectBand() {
        String responseJson = """
            {
                "pd": 0.35,
                "risk_band": "E",
                "confidence": 0.75,
                "model_version": "heuristic-v1"
            }
            """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .monthlyIncome(BigDecimal.valueOf(1000))
                .requestedAmount(BigDecimal.valueOf(50000))
                .termMonths(60)
                .creditScore(400)
                .build();

        RiskAssessmentResponse response = riskEngineClient.assessRiskBlocking(request);

        assertEquals("E", response.getRiskBand());
        assertEquals(new BigDecimal("0.35"), response.getPd());
    }

    @Test
    void assessRiskBlocking_lowRiskResponse_returnsCorrectBand() {
        String responseJson = """
            {
                "pd": 0.02,
                "risk_band": "A",
                "confidence": 0.95,
                "model_version": "heuristic-v1"
            }
            """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .monthlyIncome(BigDecimal.valueOf(10000))
                .requestedAmount(BigDecimal.valueOf(5000))
                .termMonths(12)
                .creditScore(800)
                .debtToIncomeRatio(BigDecimal.valueOf(0.1))
                .build();

        RiskAssessmentResponse response = riskEngineClient.assessRiskBlocking(request);

        assertEquals("A", response.getRiskBand());
        assertEquals(new BigDecimal("0.02"), response.getPd());
        assertEquals(new BigDecimal("0.95"), response.getConfidence());
    }

    @Test
    void assessRiskBlocking_serverError_throwsException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal server error\"}"));

        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .monthlyIncome(BigDecimal.valueOf(5000))
                .requestedAmount(BigDecimal.valueOf(10000))
                .termMonths(24)
                .build();

        assertThrows(Exception.class, () -> riskEngineClient.assessRiskBlocking(request));
    }
}
