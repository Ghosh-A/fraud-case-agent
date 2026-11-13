package com.capstone.fraudcase.agent.common;

/**
 * Structured facts distilled from tool results — the deterministic inputs every
 * processor reasons over. Built by the orchestrator from tool outputs only.
 */
public record CollectedSignals(
        boolean transactionFound,
        String authOutcome,          // SUCCESS | FAILED | UNAVAILABLE | null
        boolean duplicateFingerprint,
        boolean deviceSignalsAvailable,
        boolean deviceKnown,
        String trustLevel,           // TRUSTED | NEUTRAL | LOW | null
        boolean locationSignalsAvailable,
        double distanceKm,
        String locationAnomaly,      // NONE | LOW | MEDIUM | HIGH | null
        boolean merchantSignalsAvailable,
        String merchantRiskBand,     // LOW | MEDIUM | HIGH | null
        boolean blocklistMatch,
        int priorDisputeCount,
        int similarDisputeCount) {

    public static CollectedSignals empty() {
        return new CollectedSignals(false, null, false, false, false, null,
                false, 0, null, false, null, false, 0, 0);
    }

    /** Independently sourced high-severity anomalies. */
    public int highSeverityCount() {
        int n = 0;
        if (deviceSignalsAvailable && (!deviceKnown || "LOW".equals(trustLevel))) {
            n++;
        }
        if (locationSignalsAvailable && "HIGH".equals(locationAnomaly)) {
            n++;
        }
        if (merchantSignalsAvailable && ("HIGH".equals(merchantRiskBand) || blocklistMatch)) {
            n++;
        }
        if ("FAILED".equals(authOutcome)) {
            n++;
        }
        return n;
    }

    /** e.g. trusted long-lived device but an impossible-travel location jump. */
    public boolean contradictory() {
        return deviceSignalsAvailable && deviceKnown && "TRUSTED".equals(trustLevel)
                && locationSignalsAvailable && "HIGH".equals(locationAnomaly);
    }
}
