package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "recommendation")
public class RecommendationEntity {

    @Id
    @Column(name = "recommendation_id", length = 48)
    private String recommendationId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    /** rules | spring-ai | langchain4j | rules-fallback */
    @Column(name = "mode", nullable = false, length = 24)
    private String mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 40)
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 32)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "rationale", nullable = false, length = 2000)
    private String rationale;

    @Column(name = "evidence_ids", length = 1000)
    private String evidenceIdsJson;

    @Column(name = "missing_information", length = 1500)
    private String missingInformationJson;

    @Column(name = "rule_ids", length = 500)
    private String ruleIdsJson;

    @Column(name = "tool_trace_ids", length = 1000)
    private String toolTraceIdsJson;

    @Column(name = "human_review_required", nullable = false)
    private boolean humanReviewRequired;

    /** False when model output failed validation and was stored only for audit. */
    @Column(name = "valid", nullable = false)
    private boolean valid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getEvidenceIdsJson() {
        return evidenceIdsJson;
    }

    public void setEvidenceIdsJson(String evidenceIdsJson) {
        this.evidenceIdsJson = evidenceIdsJson;
    }

    public String getMissingInformationJson() {
        return missingInformationJson;
    }

    public void setMissingInformationJson(String missingInformationJson) {
        this.missingInformationJson = missingInformationJson;
    }

    public String getRuleIdsJson() {
        return ruleIdsJson;
    }

    public void setRuleIdsJson(String ruleIdsJson) {
        this.ruleIdsJson = ruleIdsJson;
    }

    public String getToolTraceIdsJson() {
        return toolTraceIdsJson;
    }

    public void setToolTraceIdsJson(String toolTraceIdsJson) {
        this.toolTraceIdsJson = toolTraceIdsJson;
    }

    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
