package com.capstone.fraudcase.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Allowed case transitions per the case-state contract. */
@Component
public class CaseStateMachine {

    private static final Map<CaseState, Set<CaseState>> TRANSITIONS = new EnumMap<>(CaseState.class);

    static {
        TRANSITIONS.put(CaseState.NEW, Set.of(CaseState.ENRICHING, CaseState.CLOSED));
        TRANSITIONS.put(CaseState.ENRICHING, Set.of(CaseState.READY_FOR_REVIEW, CaseState.INCOMPLETE));
        TRANSITIONS.put(CaseState.INCOMPLETE, Set.of(CaseState.ENRICHING, CaseState.CLOSED));
        TRANSITIONS.put(CaseState.READY_FOR_REVIEW, Set.of(CaseState.UNDER_REVIEW, CaseState.INCOMPLETE));
        TRANSITIONS.put(CaseState.UNDER_REVIEW, Set.of(CaseState.ROUTED, CaseState.CLOSED, CaseState.INCOMPLETE));
        TRANSITIONS.put(CaseState.ROUTED, Set.of(CaseState.CLOSED));
        TRANSITIONS.put(CaseState.CLOSED, Set.of());
    }

    public CaseState requireTransition(CaseState from, CaseState to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalCaseTransitionException(from, to);
        }
        return to;
    }

    public boolean canTransition(CaseState from, CaseState to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
