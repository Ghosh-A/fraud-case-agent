package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.TransactionClaimEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionClaimRepository extends JpaRepository<TransactionClaimEntity, String> {

    Optional<TransactionClaimEntity> findByCaseId(String caseId);
}
