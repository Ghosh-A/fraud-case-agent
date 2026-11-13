package com.capstone.fraudcase.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** Human-readable, ordered-enough identifiers for traceable artifacts. */
public final class Ids {

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100_000);

    private Ids() {
    }

    // The counter alone can collide across JVM restarts (the seed is clock-derived),
    // which previously let a "new" case silently overwrite an existing row via JPA merge.
    private static String suffix() {
        return Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
    }

    private static String next(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + suffix();
    }

    public static String caseId() {
        return "CASE-" + java.time.Year.now().getValue() + "-"
                + String.format("%05d", SEQ.incrementAndGet()) + "-" + suffix();
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
