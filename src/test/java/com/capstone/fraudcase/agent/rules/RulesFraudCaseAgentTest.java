package com.capstone.fraudcase.agent.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.fraudcase.Contexts;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.domain.RiskLevel;
import org.junit.jupiter.api.Test;

/** T01–T06 deterministic rule-path coverage. */
class RulesFraudCaseAgentTest {

    private final RulesFraudCaseAgent agent = new RulesFraudCaseAgent(new RecommendationValidator());

    @Test
    void t01_unauthorisedUseRoutesToFraudInvestigation() {
        FraudCaseRecommendation r = agent.analyze(Contexts.unauthCase());
        assertThat(r.getClassification()).isEqualTo(Classification.UNAUTHORISED_TRANSACTION);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.FRAUD_INVESTIGATION);
        assertThat(r.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.isHumanReviewRequired()).isTrue();
        assertThat(r.getRuleIds()).contains("R-03");
        assertThat(r.getMissingInformation()).contains("authentication challenge outcome");
        assertThat(r.getPolicyCitations()).isNotEmpty();
    }

    @Test
    void t02_recognisedMerchantDispute() {
        FraudCaseRecommendation r = agent.analyze(Contexts.disputeCase());
        assertThat(r.getClassification()).isEqualTo(Classification.MERCHANT_DISPUTE);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.DISPUTE_OPERATIONS);
        assertThat(r.getRuleIds()).contains("R-04");
    }

    @Test
    void t03_duplicateFingerprintRoutesToPaymentOps() {
        FraudCaseRecommendation r = agent.analyze(Contexts.duplicateCase());
        assertThat(r.getClassification()).isEqualTo(Classification.DUPLICATE_PROCESSING);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.PAYMENT_OPERATIONS);
        assertThat(r.getRuleIds()).contains("R-02");
    }

    @Test
    void t04_missingTransactionIsInsufficientEvidence() {
        FraudCaseRecommendation r = agent.analyze(Contexts.missingTxnCase());
        assertThat(r.getClassification()).isEqualTo(Classification.INSUFFICIENT_EVIDENCE);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.MANUAL_REVIEW);
        assertThat(r.getRuleIds()).contains("R-01");
        assertThat(r.getMissingInformation()).contains("transaction details unavailable");
    }

    @Test
    void t05_noPolicyForcesManualReview() {
        FraudCaseRecommendation r = agent.analyze(Contexts.noPolicyCase());
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.MANUAL_REVIEW);
        assertThat(r.getRuleIds()).contains("R-05");
        assertThat(r.getMissingInformation()).anyMatch(m -> m.contains("policy"));
    }

    @Test
    void t06_contradictionReducesConfidence() {
        FraudCaseRecommendation r = agent.analyze(Contexts.contradictoryCase());
        assertThat(r.getClassification()).isEqualTo(Classification.SUSPICIOUS_TRANSACTION);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.MANUAL_REVIEW);
        assertThat(r.getConfidence()).isLessThan(0.6);
        assertThat(r.getMissingInformation()).anyMatch(m -> m.contains("contradictory"));
    }

    @Test
    void genuineCaseDefaultsToManualReview() {
        FraudCaseRecommendation r = agent.analyze(Contexts.genuineCase());
        assertThat(r.getClassification()).isEqualTo(Classification.GENUINE_TRANSACTION);
        assertThat(r.getRecommendedRoute()).isEqualTo(Route.MANUAL_REVIEW);
        assertThat(r.getRuleIds()).contains("R-00");
    }

    @Test
    void deterministicSameInputSameOutput() {
        FraudCaseRecommendation a = agent.analyze(Contexts.unauthCase());
        FraudCaseRecommendation b = agent.analyze(Contexts.unauthCase());
        assertThat(a.getClassification()).isEqualTo(b.getClassification());
        assertThat(a.getConfidence()).isEqualTo(b.getConfidence());
        assertThat(a.getRationale()).isEqualTo(b.getRationale());
    }
}
