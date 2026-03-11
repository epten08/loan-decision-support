package com.loan.decision.riskadapter.service;

import com.loan.decision.riskadapter.controller.dto.RiskAssessmentRequest;
import com.loan.decision.riskadapter.controller.dto.RiskAssessmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class RiskEngineClient {

    private final WebClient webClient;
    private final int timeout;

    public RiskEngineClient(
            @Value("${risk-engine.base-url:http://localhost:8001}") String baseUrl,
            @Value("${risk-engine.timeout:5000}") int timeout) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.timeout = timeout;
        log.info("RiskEngineClient initialized with baseUrl: {}", baseUrl);
    }

    public Mono<RiskAssessmentResponse> assessRisk(RiskAssessmentRequest request) {
        log.debug("Sending risk assessment request to risk engine");

        return webClient.post()
                .uri("/risk/assess")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskAssessmentResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.debug("Received risk assessment: PD={}, Band={}",
                        response.getPd(), response.getRiskBand()))
                .doOnError(error -> log.error("Risk engine call failed: {}", error.getMessage()));
    }

    public RiskAssessmentResponse assessRiskBlocking(RiskAssessmentRequest request) {
        return assessRisk(request).block();
    }
}
