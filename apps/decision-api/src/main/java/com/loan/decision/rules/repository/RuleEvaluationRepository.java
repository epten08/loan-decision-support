package com.loan.decision.rules.repository;

import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluation, UUID> {

    List<RuleEvaluation> findByLoanApplicationId(UUID loanApplicationId);

    List<RuleEvaluation> findByLoanApplicationIdAndPassed(UUID loanApplicationId, boolean passed);

    List<RuleEvaluation> findByLoanApplicationIdAndSeverity(UUID loanApplicationId, RuleDefinition.RuleSeverity severity);

    boolean existsByLoanApplicationId(UUID loanApplicationId);

    void deleteByLoanApplicationId(UUID loanApplicationId);
}
