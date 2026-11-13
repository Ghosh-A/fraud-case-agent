package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.common.Hashing;
import com.capstone.fraudcase.common.Ids;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import com.capstone.fraudcase.persistence.repository.AuditEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Append-only writer for audit events. Stores hashes and summaries, never raw payloads. */
@Service
public class AuditEventWriter {

    private final AuditEventRepository repository;

    public AuditEventWriter(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEventEntity record(String caseId, AuditEventType type, ActorType actorType,
                                   String actorId, String payloadToHash, String summary) {
        AuditEventEntity event = new AuditEventEntity();
        event.setEventId(Ids.auditEventId());
        event.setCaseId(caseId);
        event.setEventType(type);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setPayloadHash(Hashing.sha256Hex(payloadToHash == null ? "" : payloadToHash));
        event.setSummary(summary == null || summary.length() <= 500
                ? summary : summary.substring(0, 500));
        event.setRecordedAt(Instant.now());
        return repository.save(event);
    }
}
