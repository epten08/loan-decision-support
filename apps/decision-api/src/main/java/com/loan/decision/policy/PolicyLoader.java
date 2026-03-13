package com.loan.decision.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible for loading and caching policy configuration from YAML.
 */
@Component
@Slf4j
public class PolicyLoader {

    private final ResourceLoader resourceLoader;
    private final String policyPath;
    private final ObjectMapper yamlMapper;

    private volatile DecisionPolicy cachedPolicy;

    public PolicyLoader(
            ResourceLoader resourceLoader,
            @Value("${decision.policy.path:classpath:policy/decision-policy.yaml}") String policyPath) {
        this.resourceLoader = resourceLoader;
        this.policyPath = policyPath;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * Loads the policy configuration at startup.
     */
    @PostConstruct
    public void init() {
        loadPolicy();
    }

    /**
     * Loads or reloads the policy from configuration file.
     *
     * @return the loaded policy
     */
    public DecisionPolicy loadPolicy() {
        try {
            Resource resource = resourceLoader.getResource(policyPath);
            if (!resource.exists()) {
                log.warn("Policy file not found at {}, using default policy", policyPath);
                cachedPolicy = DecisionPolicy.defaultPolicy();
                return cachedPolicy;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                PolicyYamlModel yamlModel = yamlMapper.readValue(inputStream, PolicyYamlModel.class);
                cachedPolicy = mapToDecisionPolicy(yamlModel);
                log.info("Loaded decision policy version {} from {}",
                        cachedPolicy.getMetadata().getVersion(), policyPath);
                return cachedPolicy;
            }
        } catch (IOException e) {
            log.error("Failed to load policy from {}: {}", policyPath, e.getMessage());
            log.warn("Using default policy due to load failure");
            cachedPolicy = DecisionPolicy.defaultPolicy();
            return cachedPolicy;
        }
    }

    /**
     * Returns the currently cached policy.
     *
     * @return the cached policy
     */
    public DecisionPolicy getCachedPolicy() {
        if (cachedPolicy == null) {
            return loadPolicy();
        }
        return cachedPolicy;
    }

    /**
     * Reloads the policy from the configuration file.
     *
     * @return the reloaded policy
     */
    public DecisionPolicy reloadPolicy() {
        log.info("Reloading policy from {}", policyPath);
        return loadPolicy();
    }

    private DecisionPolicy mapToDecisionPolicy(PolicyYamlModel yaml) {
        return DecisionPolicy.builder()
                .riskThresholds(DecisionPolicy.RiskThresholds.builder()
                        .approve(yaml.getRiskThresholds().getApprove())
                        .review(yaml.getRiskThresholds().getReview())
                        .build())
                .rules(DecisionPolicy.RulesPolicy.builder()
                        .hardFailDecline(yaml.getRules().isHardFailDecline())
                        .maxSoftFailuresForApproval(yaml.getRules().getMaxSoftFailuresForApproval())
                        .build())
                .loanLimits(DecisionPolicy.LoanLimits.builder()
                        .maxAmount(yaml.getLoanLimits().getMaxAmount())
                        .minIncome(yaml.getLoanLimits().getMinIncome())
                        .maxTermMonths(yaml.getLoanLimits().getMaxTermMonths())
                        .minTermMonths(yaml.getLoanLimits().getMinTermMonths())
                        .build())
                .creditRequirements(DecisionPolicy.CreditRequirements.builder()
                        .minCreditScore(yaml.getCreditRequirements().getMinCreditScore())
                        .maxDtiRatio(yaml.getCreditRequirements().getMaxDtiRatio())
                        .minCreditHistoryMonths(yaml.getCreditRequirements().getMinCreditHistoryMonths())
                        .build())
                .metadata(DecisionPolicy.PolicyMetadata.builder()
                        .version(yaml.getMetadata().getVersion())
                        .effectiveDate(yaml.getMetadata().getEffectiveDate())
                        .description(yaml.getMetadata().getDescription())
                        .build())
                .build();
    }
}
