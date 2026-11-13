package com.capstone.fraudcase.tools;

/**
 * The full synthetic evidence picture for one customer/transaction pair.
 * Derived deterministically from a stable hash — identical refs produce
 * identical tool results so tests and demonstrations are reproducible.
 */
public class FixtureProfile {

    // transaction
    public boolean transactionFound = true;
    public String txnStatus = "POSTED";
    public String txnChannel = "CARD_NOT_PRESENT";
    public String authOutcome = "SUCCESS"; // SUCCESS | FAILED | UNAVAILABLE
    public boolean duplicateFingerprint = false;

    // device
    public boolean deviceKnown = true;
    public int deviceAgeDays = 400;
    public String trustLevel = "TRUSTED"; // TRUSTED | NEUTRAL | LOW

    // location
    public double distanceKm = 3.0;
    public String locationAnomaly = "NONE"; // NONE | LOW | MEDIUM | HIGH
    public String currentLocation = "Bengaluru";
    public String lastTrustedLocation = "Bengaluru";

    // merchant
    public String merchantRiskBand = "LOW"; // LOW | MEDIUM | HIGH
    public boolean blocklistMatch = false;
    public String merchantCategory = "retail";

    // history
    public int priorDisputeCount = 0;
    public int similarDisputeCount = 0;
    public int lookbackDays = 365;

    public int highSeveritySignals() {
        int n = 0;
        if (!deviceKnown || "LOW".equals(trustLevel)) {
            n++;
        }
        if ("HIGH".equals(locationAnomaly)) {
            n++;
        }
        if ("HIGH".equals(merchantRiskBand) || blocklistMatch) {
            n++;
        }
        if ("FAILED".equals(authOutcome)) {
            n++;
        }
        return n;
    }
}
