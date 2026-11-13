package com.capstone.fraudcase.persistence.repository;

import com.capstone.fraudcase.persistence.entity.ChatMemoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, Long> {

    List<ChatMemoryEntity> findByCaseIdOrderBySeqAsc(String caseId);

    @Modifying
    @Query("delete from ChatMemoryEntity m where m.caseId = :caseId")
    void deleteByCaseId(String caseId);

    @Query("select coalesce(max(m.seq), -1) from ChatMemoryEntity m where m.caseId = :caseId")
    int maxSeq(String caseId);
}
