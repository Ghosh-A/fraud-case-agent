package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.agent.common.FollowUpAssistant;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Case-scoped follow-up answers through LangChain4j AI Services + case memory. */
@Component
public class LangChain4jFollowUpAssistant implements FollowUpAssistant {

    private final Lc4jChatMemoryStore memoryStore;
    private final CapstoneProperties props;

    public LangChain4jFollowUpAssistant(Lc4jChatMemoryStore memoryStore, CapstoneProperties props) {
        this.memoryStore = memoryStore;
        this.props = props;
    }

    @Override
    public String mode() {
        return "langchain4j";
    }

    @Override
    public String reply(FraudCaseContext context, String question) {
        String apiKey = LangChain4jFraudCaseAgent.envOr("OPENAI_API_KEY", null);
        if (apiKey == null || "disabled".equals(apiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY not configured for langchain4j mode");
        }
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(LangChain4jFraudCaseAgent.envOr("OPENAI_BASE_URL", "https://api.openai.com"))
                .modelName(LangChain4jFraudCaseAgent.envOr("OPENAI_CHAT_MODEL", "gpt-4o-mini"))
                .temperature(0.0)
                .timeout(Duration.ofSeconds(45))
                .build();
        FraudFollowUpService svc = AiServices.builder(FraudFollowUpService.class)
                .chatModel(model)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(props.getMemory().getMaxMessages())
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
        String prompt = "Case " + context.caseId()
                + "\n\nEVIDENCE\n" + String.join("\n", context.toolEvidenceLines())
                + "\n\nPOLICY\n" + context.policyContextText()
                + "\n\nMISSING\n" + String.join("; ", context.missingInformation())
                + "\n\nQUESTION\n" + question;
        return svc.chat(context.caseId(), prompt);
    }
}
