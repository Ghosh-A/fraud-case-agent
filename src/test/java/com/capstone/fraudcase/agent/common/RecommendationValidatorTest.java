package com.capstone.fraudcase.agent.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capstone.fraudcase.Contexts;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Output-contract validation rules (§3.4). */
class RecommendationValidatorTest {

    private final RecommendationValidator validator = new RecommendationValidator();

    private FraudCaseRecommendation valid(FraudCaseContext ctx) {
        FraudCaseRecommendation r = new FraudCaseRecommendation();
        r.setCaseId(ctx.caseId());
        r.setClassification(Classification.SUSPICIOUS_TRANSACTION);
        r.setRiskLevel(RiskLevel.MEDIUM);
        r.setRecommendedRoute(Route.MANUAL_REVIEW);
        r.setConfidence(0.5);
        r.setHumanReviewRequired(true);
        r.setEvidenceIds(List.of("EV-1"));
        r.setRationale("evidence-based rationale");
        return r;
    }

    @Test
    void acceptsValid() {
        FraudCaseContext ctx = Contexts.unauthCase();
        assertThatCode(() -> validator.requireValid(valid(ctx), ctx)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMismatchedCaseId() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = valid(ctx);
        r.setCaseId("OTHER");
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsBadConfidence() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = valid(ctx);
        r.setConfidence(1.7);
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class);
        r.setConfidence(-0.1);
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsMissingHumanReview() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = valid(ctx);
        r.setHumanReviewRequired(false);
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class)
                .hasMessageContaining("humanReviewRequired");
    }

    @Test
    void rejectsEmptyEvidenceUnlessInsufficient() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = valid(ctx);
        r.setClassification(Classification.UNAUTHORISED_TRANSACTION);
        r.setEvidenceIds(List.of());
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class);
        r.setClassification(Classification.INSUFFICIENT_EVIDENCE);
        assertThatCode(() -> validator.requireValid(r, ctx)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMalformedCitation() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = valid(ctx);
        r.setRecommendedRoute(Route.MANUAL_REVIEW);
        r.setPolicyCitations(List.of(new PolicyCitationRef("POL-1", null, "1", null, 0.5, "x")));
        assertThatThrownBy(() -> validator.requireValid(r, ctx))
                .isInstanceOf(RecommendationValidationException.class)
                .hasMessageContaining("citation");
    }

    @Test
    void t09_unknownEnumFailsAtParseLayer() {
        // JSON containing an invalid enum cannot even deserialise — equivalent rejection.
        String json = "{\"caseId\":\"C\",\"classification\":\"BOGUS\"}";
        assertThatThrownBy(() -> com.capstone.fraudcase.common.Json
                .read(json, FraudCaseRecommendation.class))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
