package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.EvidenceItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceItemRepository extends JpaRepository<EvidenceItemEntity, String> {

    List<EvidenceItemEntity> findByCaseIdOrderByObservedAtAsc(String caseId);
}
