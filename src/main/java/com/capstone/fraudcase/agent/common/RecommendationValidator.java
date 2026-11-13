package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.Route;
import org.springframework.stereotype.Component;

/**
 * Enforces the output contract before a recommendation is persisted or shown.
 * Any violation throws {@link RecommendationValidationException}; callers treat
 * that as invalid model output and fall back safely.
 */
@Component
public class RecommendationValidator {

    public FraudCaseRecommendation requireValid(FraudCaseRecommendation rec, FraudCaseContext context) {
        if (rec == null) {
            throw new RecommendationValidationException("recommendation is null");
        }
        if (rec.getCaseId() == null || !rec.getCaseId().equals(context.caseId())) {
            throw new RecommendationValidationException("caseId missing or does not match context");
        }
        if (rec.getClassification() == null) {
            throw new RecommendationValidationException("classification missing or unknown enum");
        }
        if (rec.getRiskLevel() == null) {
            throw new RecommendationValidationException("riskLevel missing or unknown enum");
        }
        if (rec.getRecommendedRoute() == null) {
            throw new RecommendationValidationException("recommendedRoute missing or unknown enum");
        }
        if (rec.getConfidence() < 0.0 || rec.getConfidence() > 1.0 || Double.isNaN(rec.getConfidence())) {
            throw new RecommendationValidationException("confidence outside 0..1: " + rec.getConfidence());
        }
        if (!rec.isHumanReviewRequired()) {
            throw new RecommendationValidationException("humanReviewRequired must be true");
        }
        boolean insufficient = rec.getClassification() == Classification.INSUFFICIENT_EVIDENCE;
        if (!insufficient && (rec.getEvidenceIds() == null || rec.getEvidenceIds().isEmpty())) {
            throw new RecommendationValidationException(
                    "at least one evidence item is required unless INSUFFICIENT_EVIDENCE");
        }
        if (rec.getPolicyCitations() != null) {
            for (PolicyCitationRef c : rec.getPolicyCitations()) {
                if (isBlank(c.getDocumentId()) || isBlank(c.getVersion()) || isBlank(c.getChunkId())) {
                    throw new RecommendationValidationException("malformed citation metadata");
                }
            }
        }
        // If grounded policy context was available, a procedural route must cite it.
        boolean proceduralRoute = rec.getRecommendedRoute() != Route.MANUAL_REVIEW;
        boolean policyWasAvailable = !context.policyCitations().isEmpty();
        if (proceduralRoute && policyWasAvailable
                && (rec.getPolicyCitations() == null || rec.getPolicyCitations().isEmpty())) {
            throw new RecommendationValidationException(
                    "procedural route requires at least one policy citation");
        }
        if (isBlank(rec.getRationale())) {
            throw new RecommendationValidationException("rationale is required");
        }
        return rec;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
