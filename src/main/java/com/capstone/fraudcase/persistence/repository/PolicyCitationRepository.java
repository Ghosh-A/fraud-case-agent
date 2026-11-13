package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.PolicyCitationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyCitationRepository extends JpaRepository<PolicyCitationEntity, String> {

    List<PolicyCitationEntity> findByRecommendationId(String recommendationId);

    List<PolicyCitationEntity> findByCaseId(String caseId);
}
