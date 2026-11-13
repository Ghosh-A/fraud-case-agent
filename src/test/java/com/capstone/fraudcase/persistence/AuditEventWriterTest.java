package com.capstone.fraudcase.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import com.capstone.fraudcase.persistence.repository.AuditEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Audit trail is append-only: records insert, hash payloads, never update. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditEventWriterTest {

    @Autowired
    private AuditEventRepository repo;

    @Test
    void eventsAreAppendedInOrder() {
        AuditEventWriter writer = new AuditEventWriter(repo);
        writer.record("CASE-1", AuditEventType.CASE_CREATED, ActorType.USER, "agent1",
                "payload", "case created");
        writer.record("CASE-1", AuditEventType.ENRICHMENT_STARTED, ActorType.SYSTEM, "system",
                "payload2", "enrichment started");

        List<AuditEventEntity> all = repo.findByCaseIdOrderByRecordedAtAsc("CASE-1");
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getEventType()).isEqualTo(AuditEventType.CASE_CREATED);
        assertThat(all.get(1).getEventType()).isEqualTo(AuditEventType.ENRICHMENT_STARTED);
        assertThat(all.get(0).getEventId()).isNotEqualTo(all.get(1).getEventId());
    }

    @Test
    void storesHashNotRawPayload() {
        AuditEventWriter writer = new AuditEventWriter(repo);
        AuditEventEntity e = writer.record("CASE-1", AuditEventType.TOOL_CALL_RECORDED,
                ActorType.SYSTEM, "system", "sensitive-payload-material", "tool ran");
        assertThat(e.getPayloadHash()).hasSize(64);
        assertThat(e.getPayloadHash()).doesNotContain("sensitive");
        assertThat(e.getSummary()).doesNotContain("sensitive-payload-material");
    }

    @Test
    void summaryIsBounded() {
        AuditEventWriter writer = new AuditEventWriter(repo);
        AuditEventEntity e = writer.record("CASE-1", AuditEventType.STATE_CHANGED,
                ActorType.SYSTEM, "sys", "x", "y".repeat(2000));
        assertThat(e.getSummary()).hasSizeLessThanOrEqualTo(500);
    }

    @Test
    void writerExposesNoMutationApi() {
        // Append-only is structural: the writer only has record().
        assertThat(AuditEventWriter.class.getDeclaredMethods())
                .allSatisfy(m -> assertThat(m.getName()).isNotIn("update", "delete", "remove"));
    }
}
