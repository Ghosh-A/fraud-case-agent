package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only: callers only insert and read. No update or delete path is used. */
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    List<AuditEventEntity> findByCaseIdOrderByRecordedAtAsc(String caseId);
}
