package com.capstone.fraudcase.domain;

public class IllegalCaseTransitionException extends RuntimeException {

    public IllegalCaseTransitionException(CaseState from, CaseState to) {
        super("Illegal case state transition: " + from + " -> " + to);
    }
}
