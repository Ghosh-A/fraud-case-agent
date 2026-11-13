package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.ToolTraceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolTraceRepository extends JpaRepository<ToolTraceEntity, String> {

    List<ToolTraceEntity> findByCaseIdOrderByCreatedAtAsc(String caseId);
}
