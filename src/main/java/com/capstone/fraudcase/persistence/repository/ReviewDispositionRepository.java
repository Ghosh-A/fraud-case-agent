package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.ReviewDispositionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDispositionRepository extends JpaRepository<ReviewDispositionEntity, String> {

    List<ReviewDispositionEntity> findByCaseIdOrderByRecordedAtDesc(String caseId);
}
