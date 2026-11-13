package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.common.Hashing;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Produces deterministic synthetic evidence. Named scenario references map to
 * fixed demonstration profiles; any other reference is derived from a stable
 * seeded hash so behaviour stays reproducible for any input.
 */
@Component
public class DeterministicFixtureGenerator {

    private final String seed;

    public DeterministicFixtureGenerator(CapstoneProperties properties) {
        this.seed = properties.getTools().getDeterministicSeed();
    }

    public FixtureProfile profileFor(String customerRef, String transactionRef) {
        FixtureProfile named = SCENARIOS.get(scenarioKey(transactionRef));
        if (named != null) {
            return named;
        }
        return hashed(customerRef, transactionRef);
    }

    public boolean hasNamedScenario(String transactionRef) {
        return SCENARIOS.containsKey(scenarioKey(transactionRef));
    }

    private String scenarioKey(String transactionRef) {
        if (transactionRef == null) {
            return "";
        }
        for (String key : SCENARIOS.keySet()) {
            if (transactionRef.startsWith(key)) {
                return key;
            }
        }
        return "";
    }

    private FixtureProfile hashed(String customerRef, String transactionRef) {
        Random rng = new Random(Hashing.stableSeed(seed, customerRef + "|" + transactionRef));
        FixtureProfile p = new FixtureProfile();
        p.transactionFound = rng.nextInt(100) < 95;
        p.txnStatus = pick(rng, "POSTED", "POSTED", "POSTED", "SETTLED", "PENDING");
        p.txnChannel = pick(rng, "CARD_NOT_PRESENT", "CARD_NOT_PRESENT", "ONLINE", "CARD_PRESENT", "MOBILE");
        p.authOutcome = pick(rng, "SUCCESS", "SUCCESS", "SUCCESS", "UNAVAILABLE", "FAILED");
        p.duplicateFingerprint = rng.nextInt(100) < 6;
        p.deviceKnown = rng.nextInt(100) < 70;
        p.deviceAgeDays = p.deviceKnown ? 30 + rng.nextInt(700) : rng.nextInt(4);
        p.trustLevel = p.deviceKnown ? pick(rng, "TRUSTED", "TRUSTED", "NEUTRAL") : "LOW";
        p.distanceKm = Math.round(rng.nextDouble() * 2500 * 10) / 10.0;
        p.locationAnomaly = p.distanceKm > 800 ? "HIGH" : p.distanceKm > 200 ? "MEDIUM" : p.distanceKm > 20 ? "LOW" : "NONE";
        p.currentLocation = pick(rng, "Bengaluru", "Mumbai", "Delhi", "Chennai", "Lisbon", "Dubai");
        p.lastTrustedLocation = pick(rng, "Bengaluru", "Mumbai", "Chennai", "Pune");
        p.merchantRiskBand = pick(rng, "LOW", "LOW", "MEDIUM", "MEDIUM", "HIGH");
        p.blocklistMatch = "HIGH".equals(p.merchantRiskBand) && rng.nextInt(100) < 40;
        p.merchantCategory = pick(rng, "retail", "electronics", "travel", "digital-goods", "food");
        p.priorDisputeCount = rng.nextInt(5);
        p.similarDisputeCount = rng.nextInt(Math.min(p.priorDisputeCount + 1, 3));
        return p;
    }

    private static String pick(Random rng, String... options) {
        return options[rng.nextInt(options.length)];
    }

    // --- Named demonstration scenarios (labelled synthetic cases) ---

    private static final Map<String, FixtureProfile> SCENARIOS = Map.of(
            // T01 — denied merchant, new device, large location anomaly.
            "TXN-SCN-UNAUTH", scenario(p -> {
                p.authOutcome = "UNAVAILABLE";
                p.deviceKnown = false;
                p.deviceAgeDays = 1;
                p.trustLevel = "LOW";
                p.distanceKm = 1870.0;
                p.locationAnomaly = "HIGH";
                p.currentLocation = "Dubai";
                p.lastTrustedLocation = "Bengaluru";
                p.merchantRiskBand = "HIGH";
                p.merchantCategory = "digital-goods";
            }),
            // T02 — recognised merchant, fulfilment/cancellation complaint.
            "TXN-SCN-DISPUTE", scenario(p -> {
                p.txnChannel = "CARD_NOT_PRESENT";
                p.merchantRiskBand = "LOW";
                p.merchantCategory = "retail";
                p.priorDisputeCount = 1;
                p.distanceKm = 4.0;
            }),
            // T03 — matching fingerprint, duplicate posting.
            "TXN-SCN-DUP", scenario(p -> {
                p.duplicateFingerprint = true;
                p.txnStatus = "POSTED";
            }),
            // T04 — transaction cannot be found at all.
            "TXN-SCN-MISSING", scenario(p -> {
                p.transactionFound = false;
                p.deviceKnown = false;
                p.deviceAgeDays = 0;
                p.trustLevel = "LOW";
                p.authOutcome = "UNAVAILABLE";
            }),
            // T05 — benign; policy abstention exercised via query not matching docs.
            "TXN-SCN-GENUINE", scenario(p -> {
                p.authOutcome = "SUCCESS";
                p.distanceKm = 1.2;
            }),
            // T06 — contradictory signals: trusted device, impossible location jump.
            "TXN-SCN-CONTRA", scenario(p -> {
                p.deviceKnown = true;
                p.deviceAgeDays = 500;
                p.trustLevel = "TRUSTED";
                p.distanceKm = 2400.0;
                p.locationAnomaly = "HIGH";
                p.currentLocation = "Lisbon";
                p.lastTrustedLocation = "Mumbai";
                p.authOutcome = "SUCCESS";
            })
    );

    private interface ProfileEdit {
        void apply(FixtureProfile p);
    }

    private static FixtureProfile scenario(ProfileEdit edit) {
        FixtureProfile p = new FixtureProfile();
        edit.apply(p);
        return p;
    }
}
