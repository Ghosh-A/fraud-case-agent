package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import java.util.ArrayList;
import java.util.List;

/**
 * The single structured-output contract shared by every FraudCaseProcessor.
 * Validated by {@link RecommendationValidator} before persistence or display.
 */
public class FraudCaseRecommendation {

    private String caseId;
    private Classification classification;
    private RiskLevel riskLevel;
    private Route recommendedRoute;
    private double confidence;
    private boolean humanReviewRequired = true;
    private List<String> evidenceIds = new ArrayList<>();
    private List<PolicyCitationRef> policyCitations = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<String> ruleIds = new ArrayList<>();
    private List<String> toolTraceIds = new ArrayList<>();
    private String rationale;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Route getRecommendedRoute() {
        return recommendedRoute;
    }

    public void setRecommendedRoute(Route recommendedRoute) {
        this.recommendedRoute = recommendedRoute;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds;
    }

    public List<PolicyCitationRef> getPolicyCitations() {
        return policyCitations;
    }

    public void setPolicyCitations(List<PolicyCitationRef> policyCitations) {
        this.policyCitations = policyCitations;
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = missingInformation;
    }

    public List<String> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds;
    }

    public List<String> getToolTraceIds() {
        return toolTraceIds;
    }

    public void setToolTraceIds(List<String> toolTraceIds) {
        this.toolTraceIds = toolTraceIds;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
