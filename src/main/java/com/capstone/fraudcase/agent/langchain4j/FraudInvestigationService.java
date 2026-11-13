package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

/** LangChain4j AI Service — declarative agent bound to case memory. */
public interface FraudInvestigationService {

    @SystemMessage(fromResource = "prompts/fraud-system-lc4j.txt")
    FraudCaseRecommendation analyze(
            @MemoryId String caseId,
            @V("outputSchema") String outputSchema,
            String userPrompt);
}
