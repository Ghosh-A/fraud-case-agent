package com.capstone.fraudcase.agent.common;

/**
 * Case-scoped follow-up answering. Implementations exist per adapter; all
 * answers stay grounded in the case evidence/citations and use case-keyed
 * memory only.
 */
public interface FollowUpAssistant {

    /** Mode label this assistant handles, or null when unavailable. */
    String mode();

    String reply(FraudCaseContext context, String question);
}
