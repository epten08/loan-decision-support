package com.loan.decision.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for policy management operations.
 * Provides endpoints to view and reload policy configuration.
 */
@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    /**
     * Returns the currently active decision policy.
     */
    @GetMapping
    public ResponseEntity<DecisionPolicy> getCurrentPolicy() {
        log.info("Fetching current policy");
        return ResponseEntity.ok(policyService.getCurrentPolicy());
    }

    /**
     * Returns just the policy version.
     */
    @GetMapping("/version")
    public ResponseEntity<PolicyVersionResponse> getPolicyVersion() {
        DecisionPolicy policy = policyService.getCurrentPolicy();
        return ResponseEntity.ok(new PolicyVersionResponse(
                policy.getMetadata().getVersion(),
                policy.getMetadata().getEffectiveDate(),
                policy.getMetadata().getDescription()
        ));
    }

    /**
     * Reloads the policy from configuration.
     * This allows picking up changes without restarting the service.
     */
    @PostMapping("/reload")
    public ResponseEntity<DecisionPolicy> reloadPolicy() {
        log.info("Policy reload requested via API");
        DecisionPolicy policy = policyService.reloadPolicy();
        return ResponseEntity.ok(policy);
    }

    /**
     * Returns only the risk thresholds portion of the policy.
     */
    @GetMapping("/thresholds")
    public ResponseEntity<DecisionPolicy.RiskThresholds> getRiskThresholds() {
        return ResponseEntity.ok(policyService.getCurrentPolicy().getRiskThresholds());
    }

    /**
     * Returns only the loan limits portion of the policy.
     */
    @GetMapping("/loan-limits")
    public ResponseEntity<DecisionPolicy.LoanLimits> getLoanLimits() {
        return ResponseEntity.ok(policyService.getCurrentPolicy().getLoanLimits());
    }

    /**
     * Returns only the credit requirements portion of the policy.
     */
    @GetMapping("/credit-requirements")
    public ResponseEntity<DecisionPolicy.CreditRequirements> getCreditRequirements() {
        return ResponseEntity.ok(policyService.getCurrentPolicy().getCreditRequirements());
    }

    public record PolicyVersionResponse(String version, String effectiveDate, String description) {}
}
