package com.loan.decision.creditprofile.repository;

import com.loan.decision.creditprofile.model.CreditProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditProfileRepository extends JpaRepository<CreditProfile, UUID> {

    Optional<CreditProfile> findByLoanApplicationId(UUID loanApplicationId);

    boolean existsByLoanApplicationId(UUID loanApplicationId);
}
