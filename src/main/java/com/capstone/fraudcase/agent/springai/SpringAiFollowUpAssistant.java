package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.agent.common.FollowUpAssistant;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Case-scoped follow-up answers through the Spring AI chat client + case memory. */
@Component
public class SpringAiFollowUpAssistant implements FollowUpAssistant {

    private final ObjectProvider<ChatModel> chatModel;
    private final ChatMemory chatMemory;

    public SpringAiFollowUpAssistant(ObjectProvider<ChatModel> chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

    @Override
    public String mode() {
        return "spring-ai";
    }

    @Override
    public String reply(FraudCaseContext context, String question) {
        ChatModel model = chatModel.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("spring-ai chat model unavailable");
        }
        ChatClient client = ChatClient.builder(model)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        String prompt = "You are answering a follow-up question inside fraud case " + context.caseId()
                + ". Use ONLY the evidence and citations below; never invent facts.\n\nEVIDENCE\n"
                + String.join("\n", context.toolEvidenceLines())
                + "\n\nPOLICY\n" + context.policyContextText()
                + "\n\nMISSING\n" + String.join("; ", context.missingInformation())
                + "\n\nQUESTION\n" + question;
        return client.prompt()
                .system("Answer concisely. Cite evidence IDs / chunk IDs. No actions may be taken.")
                .user(prompt)
                .advisors(a -> a.param(SpringAiFraudCaseAgent.CONVERSATION_ID_PARAM, context.caseId()))
                .call()
                .content();
    }
}
