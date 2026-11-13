package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import com.capstone.fraudcase.agent.common.PromptTemplates;
import com.capstone.fraudcase.agent.common.RecommendationValidationException;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.tools.EvidenceTools;
import com.capstone.fraudcase.tools.ModelFacingTools;
import com.capstone.fraudcase.tools.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Spring AI implementation of the FraudCaseProcessor contract. */
@Component
public class SpringAiFraudCaseAgent implements FraudCaseProcessor {

    public static final String MODE = "spring-ai";
    private static final Logger log = LoggerFactory.getLogger(SpringAiFraudCaseAgent.class);

    /** Conversation-scoping parameter keys used by chat-memory advisors. */
    static final String CONVERSATION_ID_PARAM = "chat_memory_conversation_id";
    static final String RETRIEVE_SIZE_PARAM = "chat_memory_response_size";

    private final ObjectProvider<ChatModel> chatModel;
    private final ChatMemory chatMemory;
    private final PromptTemplates prompts;
    private final RecommendationValidator validator;
    private final EvidenceTools tools;
    private final ToolExecutor executor;
    private final CapstoneProperties props;

    public SpringAiFraudCaseAgent(ObjectProvider<ChatModel> chatModel, ChatMemory chatMemory,
                                  PromptTemplates prompts, RecommendationValidator validator,
                                  EvidenceTools tools, ToolExecutor executor,
                                  CapstoneProperties props) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.prompts = prompts;
        this.validator = validator;
        this.tools = tools;
        this.executor = executor;
        this.props = props;
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext context) {
        ChatModel model = chatModel.getIfAvailable();
        if (model == null) {
            throw new ProcessorUnavailableException(
                    "spring-ai chat model not configured (spring.ai.model.chat != openai or no API key)");
        }
        ChatClient client = ChatClient.builder(model)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        ModelFacingTools modelTools = ModelFacingTools.forCase(tools, executor, context.caseId(),
                context.summary().maskedCustomerRef(), context.summary().transactionRef());
        try {
            FraudCaseRecommendation result = client.prompt()
                    .system(prompts.renderSystem())
                    .user(prompts.renderUser(context))
                    .tools(modelTools)
                    .advisors(a -> a.param(CONVERSATION_ID_PARAM, context.caseId())
                            .param(RETRIEVE_SIZE_PARAM, props.getMemory().getMaxMessages()))
                    .call()
                    .entity(FraudCaseRecommendation.class);
            return validator.requireValid(result, context);
        } catch (RecommendationValidationException | ProcessorUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessorUnavailableException("spring-ai call failed: " + e.getMessage(), e);
        }
    }
}
