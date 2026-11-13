package com.capstone.fraudcase.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingValidatorTest {

    private final MaskingValidator v = new MaskingValidator();

    @Test
    void acceptsMaskedReferences() {
        assertThat(v.isMaskedReference("CUST-••••-4821")).isTrue();
        assertThat(v.isMaskedReference("TXN-2026-784102")).isTrue();
        assertThat(v.isMaskedReference("NORTHSTAR DIGITAL ••••73")).isTrue();
    }

    @Test
    void rejectsRawPan() {
        assertThat(v.isMaskedReference("4111111111111111")).isFalse();
        assertThat(v.isMaskedReference("4111-1111-1111-1111")).isFalse();
        assertThat(v.containsUnmaskedIdentifier("card 4111 1111 1111 1111")).isTrue();
    }

    @Test
    void rejectsBlankAndOddInput() {
        assertThat(v.isMaskedReference("")).isFalse();
        assertThat(v.isMaskedReference(null)).isFalse();
        assertThat(v.isMaskedReference("<script>")).isFalse();
    }

    @Test
    void detectsMarkupInStatements() {
        assertThat(v.containsMarkup("<script>alert(1)</script>")).isTrue();
        assertThat(v.containsMarkup("<img src=x onerror=1>")).isTrue();
        assertThat(v.containsMarkup("I do not recognise this purchase")).isFalse();
    }
}
