package com.capstone.fraudcase.agent.common;

/**
 * Framework-neutral processor contract. Implementations: rules (deterministic
 * fallback), Spring AI and LangChain4j. All produce the same validated schema.
 */
public interface FraudCaseProcessor {

    FraudCaseRecommendation analyze(FraudCaseContext context);

    /** Stable mode label persisted with each recommendation. */
    String mode();
}
