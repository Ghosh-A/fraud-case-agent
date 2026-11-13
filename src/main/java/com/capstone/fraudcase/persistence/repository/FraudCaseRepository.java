package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.FraudCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseRepository extends JpaRepository<FraudCaseEntity, String> {
}
