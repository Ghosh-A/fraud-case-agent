package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.RecommendationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, String> {

    Optional<RecommendationEntity> findTopByCaseIdAndValidTrueOrderByCreatedAtDesc(String caseId);
}
