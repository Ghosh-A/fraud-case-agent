package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.CaseSummary;
import com.capstone.fraudcase.agent.common.CollectedSignals;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Builders for labelled synthetic test cases (T01–T12). */
public final class Contexts {

    public static final PolicyCitationRef CNP_CITATION = new PolicyCitationRef(
            "POL-CNP-017", "3.2", "3.2 Initial fraud routing", "pol-cnp-017-c1", 0.77,
            "When a customer denies a card-not-present transaction and independent signals show "
                    + "a new device or anomalous location, route to FRAUD_INVESTIGATION.");

    public static final PolicyCitationRef DSP_CITATION = new PolicyCitationRef(
            "POL-DSP-004", "1.7", "1.2 Fulfilment", "pol-dsp-004-c1", 0.81,
            "Customer reports non-delivery or cancellation that still posted: MERCHANT_DISPUTE.");

    public static final PolicyCitationRef PAY_CITATION = new PolicyCitationRef(
            "POL-PAY-009", "2.1", "2.1 Identifying duplicate processing", "pol-pay-009-c0", 0.79,
            "Duplicate processing exists when the same payment fingerprint posts more than once.");

    private Contexts() {
    }

    public static CaseSummary summary(String txnRef, IssueType issue, boolean merchantRecognised) {
        return new CaseSummary("CASE-TEST-1", "CUST-••••-4821", "IN-KA", txnRef,
                new BigDecimal("48750.00"), "INR", Instant.parse("2026-11-06T03:12:00Z"),
                TransactionStatus.POSTED, TransactionChannel.CARD_NOT_PRESENT,
                "NORTHSTAR DIGITAL ••••73", issue,
                "I do not recognise this purchase and still have my card.",
                CardPossession.HAS_CARD, merchantRecognised, Instant.parse("2026-11-13T04:30:00Z"));
    }

    public static FraudCaseContext context(CaseSummary summary, CollectedSignals signals,
                                           List<PolicyCitationRef> citations) {
        FraudCaseContext ctx = new FraudCaseContext(summary, "test goal");
        ctx.setSignals(signals);
        ctx.addEvidence("EV-1", "Transaction details — posted");
        ctx.addEvidence("EV-2", "Device signals — observed");
        ctx.addEvidence("EV-3", "Location comparison — observed");
        ctx.toolTraceIds().addAll(List.of("TR-1", "TR-2", "TR-3"));
        ctx.policyCitations().addAll(citations);
        return ctx;
    }

    /** T01 — denied merchant, new device, large location anomaly. */
    public static FraudCaseContext unauthCase() {
        CollectedSignals s = new CollectedSignals(true, "UNAVAILABLE", false,
                true, false, "LOW", true, 1870.0, "HIGH",
                true, "HIGH", false, 0, 0);
        return context(summary("TXN-SCN-UNAUTH-001", IssueType.MERCHANT_NOT_RECOGNISED, false),
                s, List.of(CNP_CITATION));
    }

    /** T02 — recognised merchant, cancellation complaint. */
    public static FraudCaseContext disputeCase() {
        CollectedSignals s = new CollectedSignals(true, "SUCCESS", false,
                true, true, "TRUSTED", true, 4.0, "NONE",
                true, "LOW", false, 1, 0);
        return context(summary("TXN-SCN-DISPUTE-001", IssueType.CANCELLATION, true),
                s, List.of(DSP_CITATION));
    }

    /** T03 — matching fingerprint, duplicate posting. */
    public static FraudCaseContext duplicateCase() {
        CollectedSignals s = new CollectedSignals(true, "SUCCESS", true,
                true, true, "TRUSTED", true, 2.0, "NONE",
                true, "LOW", false, 0, 0);
        return context(summary("TXN-SCN-DUP-001", IssueType.DUPLICATE_CHARGE, true),
                s, List.of(PAY_CITATION));
    }

    /** T04 — transaction missing entirely. */
    public static FraudCaseContext missingTxnCase() {
        CollectedSignals s = new CollectedSignals(false, null, false,
                false, false, null, false, 0, null,
                false, null, false, 0, 0);
        FraudCaseContext ctx = context(summary("TXN-SCN-MISSING-001", IssueType.OTHER, false),
                s, List.of());
        ctx.addMissing("transaction details unavailable");
        return ctx;
    }

    /** T05 — evidence fine but no policy passage above threshold. */
    public static FraudCaseContext noPolicyCase() {
        CollectedSignals s = new CollectedSignals(true, "SUCCESS", false,
                true, false, "LOW", true, 900.0, "HIGH",
                true, "HIGH", false, 0, 0);
        return context(summary("TXN-SCN-GENUINE-001", IssueType.MERCHANT_NOT_RECOGNISED, false),
                s, List.of());
    }

    /** T06 — contradictory signals: trusted device + impossible travel. */
    public static FraudCaseContext contradictoryCase() {
        CollectedSignals s = new CollectedSignals(true, "SUCCESS", false,
                true, true, "TRUSTED", true, 2400.0, "HIGH",
                true, "LOW", false, 0, 0);
        return context(summary("TXN-SCN-CONTRA-001", IssueType.OTHER, true),
                s, List.of(CNP_CITATION));
    }

    /** Benign case. */
    public static FraudCaseContext genuineCase() {
        CollectedSignals s = new CollectedSignals(true, "SUCCESS", false,
                true, true, "TRUSTED", true, 1.2, "NONE",
                true, "LOW", false, 0, 0);
        return context(summary("TXN-SCN-GENUINE-002", IssueType.OTHER, true),
                s, List.of(DSP_CITATION));
    }
}
