package com.capstone.fraudcase.common;

import java.util.concurrent.atomic.AtomicLong;

/** Human-readable, ordered-enough identifiers for traceable artifacts. */
public final class Ids {

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100_000);

    private Ids() {
    }

    private static String next(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet();
    }

    public static String caseId() {
        return "CASE-" + java.time.Year.now().getValue() + "-" + String.format("%05d", SEQ.incrementAndGet());
    }

    public static String claimId() {
        return next("CLM");
    }

    public static String evidenceId() {
        return next("EV");
    }

    public static String traceId() {
        return next("TR");
    }

    public static String recommendationId() {
        return next("REC");
    }

    public static String citationId() {
        return next("CIT");
    }

    public static String reviewId() {
        return next("REV");
    }

    public static String auditEventId() {
        return next("AE");
    }

    public static String toolResultId() {
        return next("TRES");
    }
}
