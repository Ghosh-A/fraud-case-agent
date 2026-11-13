package com.capstone.fraudcase.domain;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String caseId) {
        super("Case not found: " + caseId);
    }
}
