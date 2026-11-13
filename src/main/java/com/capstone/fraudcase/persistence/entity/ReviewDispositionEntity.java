package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.Disposition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "review_disposition")
public class ReviewDispositionEntity {

    @Id
    @Column(name = "review_id", length = 48)
    private String reviewId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    @Column(name = "reviewer_id", nullable = false, length = 64)
    private String reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 24)
    private Disposition disposition;

    @Column(name = "reason", nullable = false, length = 1500)
    private String reason;

    /** Route selected when disposition is REROUTE/OVERRIDE/ESCALATE. */
    @Column(name = "chosen_route", length = 32)
    private String chosenRoute;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChosenRoute() {
        return chosenRoute;
    }

    public void setChosenRoute(String chosenRoute) {
        this.chosenRoute = chosenRoute;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
