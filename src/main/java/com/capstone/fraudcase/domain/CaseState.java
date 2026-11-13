package com.capstone.fraudcase.domain;

public enum CaseState {
    NEW,
    ENRICHING,
    INCOMPLETE,
    READY_FOR_REVIEW,
    UNDER_REVIEW,
    ROUTED,
    CLOSED
}
