package com.capstone.fraudcase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CaseStateMachineTest {

    private final CaseStateMachine sm = new CaseStateMachine();

    @Test
    void allowsHappyPath() {
        assertThat(sm.requireTransition(CaseState.NEW, CaseState.ENRICHING)).isEqualTo(CaseState.ENRICHING);
        assertThat(sm.requireTransition(CaseState.ENRICHING, CaseState.READY_FOR_REVIEW))
                .isEqualTo(CaseState.READY_FOR_REVIEW);
        assertThat(sm.requireTransition(CaseState.READY_FOR_REVIEW, CaseState.UNDER_REVIEW))
                .isEqualTo(CaseState.UNDER_REVIEW);
        assertThat(sm.requireTransition(CaseState.UNDER_REVIEW, CaseState.ROUTED))
                .isEqualTo(CaseState.ROUTED);
        assertThat(sm.requireTransition(CaseState.ROUTED, CaseState.CLOSED)).isEqualTo(CaseState.CLOSED);
    }

    @Test
    void allowsIncompleteRecovery() {
        assertThat(sm.canTransition(CaseState.ENRICHING, CaseState.INCOMPLETE)).isTrue();
        assertThat(sm.canTransition(CaseState.INCOMPLETE, CaseState.ENRICHING)).isTrue();
        assertThat(sm.canTransition(CaseState.INCOMPLETE, CaseState.CLOSED)).isTrue();
    }

    @Test
    void rejectsIllegalTransitions() {
        assertThatThrownBy(() -> sm.requireTransition(CaseState.NEW, CaseState.ROUTED))
                .isInstanceOf(IllegalCaseTransitionException.class);
        assertThatThrownBy(() -> sm.requireTransition(CaseState.CLOSED, CaseState.ENRICHING))
                .isInstanceOf(IllegalCaseTransitionException.class);
        assertThatThrownBy(() -> sm.requireTransition(CaseState.READY_FOR_REVIEW, CaseState.ROUTED))
                .isInstanceOf(IllegalCaseTransitionException.class);
    }
}
