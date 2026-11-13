package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.CaseState;
import com.capstone.fraudcase.domain.IntakeChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fraud_case")
public class FraudCaseEntity {

    @Id
    @Column(name = "case_id", length = 40)
    private String caseId;

    @Column(name = "masked_customer_ref", nullable = false, length = 64)
    private String maskedCustomerRef;

    @Column(name = "home_region", length = 64)
    private String homeRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CaseState status;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_channel", nullable = false, length = 24)
    private IntakeChannel intakeChannel;

    @Column(name = "analyst_id", nullable = false, length = 64)
    private String analystId;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getMaskedCustomerRef() {
        return maskedCustomerRef;
    }

    public void setMaskedCustomerRef(String maskedCustomerRef) {
        this.maskedCustomerRef = maskedCustomerRef;
    }

    public String getHomeRegion() {
        return homeRegion;
    }

    public void setHomeRegion(String homeRegion) {
        this.homeRegion = homeRegion;
    }

    public CaseState getStatus() {
        return status;
    }

    public void setStatus(CaseState status) {
        this.status = status;
    }

    public IntakeChannel getIntakeChannel() {
        return intakeChannel;
    }

    public void setIntakeChannel(IntakeChannel intakeChannel) {
        this.intakeChannel = intakeChannel;
    }

    public String getAnalystId() {
        return analystId;
    }

    public void setAnalystId(String analystId) {
        this.analystId = analystId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
