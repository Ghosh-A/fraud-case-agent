package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only audit record. No update or delete path is exposed. */
@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    @Column(name = "event_id", length = 48)
    private String eventId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    /** SHA-256 of the canonical payload — proves content without storing it. */
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
