package com.loan.decision.decisioning.repository;

import com.loan.decision.decisioning.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {

    Optional<Decision> findByLoanApplicationId(UUID loanApplicationId);

    boolean existsByLoanApplicationId(UUID loanApplicationId);

    List<Decision> findByOutcome(Decision.DecisionOutcome outcome);
}
