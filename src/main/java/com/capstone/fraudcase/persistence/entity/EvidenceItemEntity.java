package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.EvidenceType;
import com.capstone.fraudcase.domain.Provenance;
import com.capstone.fraudcase.domain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evidence_item")
public class EvidenceItemEntity {

    @Id
    @Column(name = "evidence_id", length = 48)
    private String evidenceId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private EvidenceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "value_summary", nullable = false, length = 1200)
    private String valueSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 16)
    private Provenance provenance;

    /** Trace, tool-result or citation identifier backing this item. */
    @Column(name = "source_ref", length = 64)
    private String sourceRef;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public EvidenceType getType() {
        return type;
    }

    public void setType(EvidenceType type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValueSummary() {
        return valueSummary;
    }

    public void setValueSummary(String valueSummary) {
        this.valueSummary = valueSummary;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }
}
