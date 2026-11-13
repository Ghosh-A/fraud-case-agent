package com.capstone.fraudcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.Route;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Evaluation harness — runs the labelled fixture set through the rules
 * processor and reports schema-valid %, abstention rate, and citation recall.
 * Kept as a test so regressions fail the build, not just a report.
 */
class EvaluationHarnessTest {

    private final RulesFraudCaseAgent agent = new RulesFraudCaseAgent(new RecommendationValidator());

    /** Label → context supplier → expected route. */
    private record EvalCase(String label, FraudCaseContext ctx, Route expectedRoute,
                            boolean expectsAbstention) {
    }

    @Test
    void labelledFixtureSet() {
        EvalCase[] set = {
                new EvalCase("unauth", Contexts.unauthCase(), Route.FRAUD_INVESTIGATION, false),
                new EvalCase("dispute", Contexts.disputeCase(), Route.DISPUTE_OPERATIONS, false),
                new EvalCase("duplicate", Contexts.duplicateCase(), Route.PAYMENT_OPERATIONS, false),
                new EvalCase("missing-txn", Contexts.missingTxnCase(), Route.MANUAL_REVIEW, true),
                new EvalCase("no-policy", Contexts.noPolicyCase(), Route.MANUAL_REVIEW, true),
                new EvalCase("contradictory", Contexts.contradictoryCase(), Route.MANUAL_REVIEW, true),
                new EvalCase("genuine", Contexts.genuineCase(), Route.MANUAL_REVIEW, false),
        };

        int schemaValid = 0, routeCorrect = 0, abstainedCorrectly = 0, citedWhenExpected = 0;
        int expectedCitations = 0;
        Map<String, String> results = new LinkedHashMap<>();

        for (EvalCase ec : set) {
            FraudCaseRecommendation r = agent.analyze(ec.ctx());
            boolean valid = isSchemaValid(r, ec.ctx());
            schemaValid += valid ? 1 : 0;
            routeCorrect += r.getRecommendedRoute() == ec.expectedRoute() ? 1 : 0;
            boolean abstained = r.getClassification() == Classification.INSUFFICIENT_EVIDENCE
                    || r.getRecommendedRoute() == Route.MANUAL_REVIEW;
            abstainedCorrectly += abstained == ec.expectsAbstention()
                    || !ec.expectsAbstention() && r.getRecommendedRoute() == ec.expectedRoute() ? 1 : 0;
            if (!ec.expectsAbstention()) {
                expectedCitations++;
                citedWhenExpected += r.getPolicyCitations().isEmpty() ? 0 : 1;
            }
            results.put(ec.label, r.getClassification() + " -> " + r.getRecommendedRoute());
        }

        double schemaValidPct = 100.0 * schemaValid / set.length;
        double routeAccuracy = 100.0 * routeCorrect / set.length;
        double citationRecall = expectedCitations == 0 ? 0
                : 100.0 * citedWhenExpected / expectedCitations;

        System.out.println("=== evaluation summary ===");
        results.forEach((k, v) -> System.out.printf("  %-14s %s%n", k, v));
        System.out.printf("  schema-valid: %.0f%%%n  route accuracy: %.0f%%%n"
                        + "  abstention correctness: %d/%d%n  citation recall: %.0f%%%n",
                schemaValidPct, routeAccuracy, abstainedCorrectly, set.length, citationRecall);

        assertThat(schemaValidPct).isEqualTo(100.0);
        assertThat(routeAccuracy).isEqualTo(100.0);
        assertThat(citationRecall).isEqualTo(100.0);
    }

    private boolean isSchemaValid(FraudCaseRecommendation r, FraudCaseContext ctx) {
        try {
            new RecommendationValidator().requireValid(r, ctx);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
