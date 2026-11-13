package com.capstone.fraudcase.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.fraudcase.CapstoneProperties;
import org.junit.jupiter.api.Test;

class DeterministicFixtureGeneratorTest {

    private final DeterministicFixtureGenerator gen;

    DeterministicFixtureGeneratorTest() {
        CapstoneProperties props = new CapstoneProperties();
        this.gen = new DeterministicFixtureGenerator(props);
    }

    @Test
    void identicalInputProducesIdenticalProfile() {
        FixtureProfile a = gen.profileFor("CUST-A", "TXN-XYZ");
        FixtureProfile b = gen.profileFor("CUST-A", "TXN-XYZ");
        assertThat(a.deviceKnown).isEqualTo(b.deviceKnown);
        assertThat(a.distanceKm).isEqualTo(b.distanceKm);
        assertThat(a.merchantRiskBand).isEqualTo(b.merchantRiskBand);
        assertThat(a.priorDisputeCount).isEqualTo(b.priorDisputeCount);
        assertThat(a.authOutcome).isEqualTo(b.authOutcome);
        assertThat(a.duplicateFingerprint).isEqualTo(b.duplicateFingerprint);
    }

    @Test
    void differentInputProducesDifferentProfile() {
        FixtureProfile a = gen.profileFor("CUST-A", "TXN-1");
        FixtureProfile b = gen.profileFor("CUST-B", "TXN-2");
        assertThat(a.deviceAgeDays == b.deviceAgeDays && a.distanceKm == b.distanceKm
                && a.merchantRiskBand.equals(b.merchantRiskBand)).isFalse();
    }

    @Test
    void namedScenarioOverridesHash() {
        FixtureProfile unauth = gen.profileFor("ANY", "TXN-SCN-UNAUTH-001");
        assertThat(unauth.deviceKnown).isFalse();
        assertThat(unauth.locationAnomaly).isEqualTo("HIGH");
        assertThat(unauth.authOutcome).isEqualTo("UNAVAILABLE");

        FixtureProfile dup = gen.profileFor("ANY", "TXN-SCN-DUP-007");
        assertThat(dup.duplicateFingerprint).isTrue();

        FixtureProfile missing = gen.profileFor("ANY", "TXN-SCN-MISSING-1");
        assertThat(missing.transactionFound).isFalse();
    }
}
