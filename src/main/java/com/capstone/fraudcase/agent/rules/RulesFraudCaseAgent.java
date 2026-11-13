package com.capstone.fraudcase.agent.rules;

import com.capstone.fraudcase.agent.common.CollectedSignals;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback processor. No model dependency — usable whenever the
 * model is disabled or unavailable. Every result lists the rule IDs and the
 * exact input facts it was decided on.
 */
@Component
public class RulesFraudCaseAgent implements FraudCaseProcessor {

    public static final String MODE = "rules";
    private static final Set<IssueType> MERCHANT_ISSUES =
            EnumSet.of(IssueType.FULFILMENT, IssueType.CANCELLATION, IssueType.AMOUNT_DISPUTE);

    private final RecommendationValidator validator;

    public RulesFraudCaseAgent(RecommendationValidator validator) {
        this.validator = validator;
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext ctx) {
        CollectedSignals s = ctx.signals();
        List<String> rules = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        List<String> missing = new ArrayList<>(ctx.missingInformation());

        Classification classification;
        Route route;
        RiskLevel risk;
        double confidence;

        if (!s.transactionFound()) {
            rules.add("R-01");
            classification = Classification.INSUFFICIENT_EVIDENCE;
            route = Route.MANUAL_REVIEW;
            risk = RiskLevel.MEDIUM;
            confidence = 0.9;
            missing.add("transaction details unavailable");
            facts.add("getTransactionDetails.found=false");
        } else if (s.duplicateFingerprint()) {
            rules.add("R-02");
            classification = Classification.DUPLICATE_PROCESSING;
            route = Route.PAYMENT_OPERATIONS;
            risk = RiskLevel.MEDIUM;
            confidence = 0.93;
            facts.add("duplicateFingerprint=true");
        } else if (s.contradictory()) {
            rules.add("R-06");
            classification = Classification.SUSPICIOUS_TRANSACTION;
            route = Route.MANUAL_REVIEW;
            risk = RiskLevel.MEDIUM;
            confidence = 0.45;
            missing.add("contradictory device/location signals");
            facts.add("deviceKnown=true&trustLevel=TRUSTED");
            facts.add("locationAnomaly=HIGH distanceKm=" + s.distanceKm());
        } else if (s.highSeverityCount() >= 2) {
            rules.add("R-03");
            boolean denied = !ctx.summary().merchantRecognised()
                    || ctx.summary().issueType() == IssueType.MERCHANT_NOT_RECOGNISED
                    || ctx.summary().issueType() == IssueType.UNAUTHORISED_USE;
            classification = denied
                    ? Classification.UNAUTHORISED_TRANSACTION : Classification.SUSPICIOUS_TRANSACTION;
            route = Route.FRAUD_INVESTIGATION;
            risk = RiskLevel.HIGH;
            confidence = 0.8;
            facts.add("highSeveritySignals=" + s.highSeverityCount());
            facts.add("deviceKnown=" + s.deviceKnown() + ";trustLevel=" + s.trustLevel());
            facts.add("locationAnomaly=" + s.locationAnomaly() + ";distanceKm=" + s.distanceKm());
            facts.add("merchantRiskBand=" + s.merchantRiskBand() + ";blocklist=" + s.blocklistMatch());
            facts.add("authOutcome=" + s.authOutcome());
        } else if (ctx.summary().merchantRecognised() && MERCHANT_ISSUES.contains(ctx.summary().issueType())) {
            rules.add("R-04");
            classification = Classification.MERCHANT_DISPUTE;
            route = Route.DISPUTE_OPERATIONS;
            risk = RiskLevel.MEDIUM;
            confidence = 0.85;
            facts.add("merchantRecognised=true");
            facts.add("issueType=" + ctx.summary().issueType());
        } else {
            rules.add("R-00");
            classification = Classification.GENUINE_TRANSACTION;
            route = Route.MANUAL_REVIEW;
            risk = RiskLevel.LOW;
            confidence = 0.6;
            facts.add("no fraud or dispute rule matched");
        }

        // R-05 — no policy passage above threshold: never invent a procedure.
        if (ctx.policyCitations().isEmpty()) {
            rules.add("R-05");
            if (route != Route.MANUAL_REVIEW) {
                facts.add("policyCitations=0 -> route downgraded to MANUAL_REVIEW");
            }
            route = Route.MANUAL_REVIEW;
            missing.add("no policy passage above retrieval threshold");
        }
        if ("UNAVAILABLE".equals(s.authOutcome()) || s.authOutcome() == null) {
            missing.add("authentication challenge outcome");
        }

        FraudCaseRecommendation r = new FraudCaseRecommendation();
        r.setCaseId(ctx.caseId());
        r.setClassification(classification);
        r.setRiskLevel(risk);
        r.setRecommendedRoute(route);
        r.setConfidence(Math.max(0.3, confidence - (missing.isEmpty() ? 0 : 0.05 * missing.size())));
        r.setHumanReviewRequired(true);
        r.setEvidenceIds(ctx.evidenceIds());
        r.setPolicyCitations(ctx.policyCitations());
        r.setMissingInformation(missing.stream().distinct().toList());
        r.setRuleIds(rules);
        r.setToolTraceIds(ctx.toolTraceIds());
        r.setRationale("Deterministic rules " + rules + " on facts: " + String.join("; ", facts)
                + ". Advisory only — human reviewer decides.");
        return validator.requireValid(r, ctx);
    }
}
