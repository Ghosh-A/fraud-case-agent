package com.capstone.fraudcase.agent.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;

/** Case-scoped follow-up chat through LangChain4j AI Services. */
public interface FraudFollowUpService {

    @SystemMessage(fromResource = "prompts/fraud-followup-system.txt")
    String chat(@MemoryId String caseId, String userMessage);
}
