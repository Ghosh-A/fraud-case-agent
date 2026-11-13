package com.capstone.fraudcase.agent.langchain4j;

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
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** LangChain4j implementation of the FraudCaseProcessor contract. */
@Component
public class LangChain4jFraudCaseAgent implements FraudCaseProcessor {

    public static final String MODE = "langchain4j";
    private static final Logger log = LoggerFactory.getLogger(LangChain4jFraudCaseAgent.class);

    private final PromptTemplates prompts;
    private final RecommendationValidator validator;
    private final EvidenceTools tools;
    private final ToolExecutor executor;
    private final Lc4jChatMemoryStore memoryStore;
    private final CapstoneProperties props;
    /** Test seam — when set, analyze() uses this service instead of a live model. */
    private volatile FraudInvestigationService injectedService;

    public LangChain4jFraudCaseAgent(PromptTemplates prompts, RecommendationValidator validator,
                                   EvidenceTools tools, ToolExecutor executor,
                                   Lc4jChatMemoryStore memoryStore, CapstoneProperties props) {
        this.prompts = prompts;
        this.validator = validator;
        this.tools = tools;
        this.executor = executor;
        this.memoryStore = memoryStore;
        this.props = props;
    }

    /** For tests: inject a stubbed AI service. */
    public void setServiceForTesting(FraudInvestigationService service) {
        this.injectedService = service;
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext context) {
        FraudInvestigationService svc = injectedService != null ? injectedService
                : AiServices.builder(FraudInvestigationService.class)
                .chatModel(openAiModel())
                .tools(ModelFacingTools.forCase(tools, executor, context.caseId(),
                        context.summary().maskedCustomerRef(), context.summary().transactionRef()))
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(props.getMemory().getMaxMessages())
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
        try {
            FraudCaseRecommendation result = svc.analyze(
                    context.caseId(), prompts.outputSchema(), prompts.renderUser(context));
            return validator.requireValid(result, context);
        } catch (RecommendationValidationException | ProcessorUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProcessorUnavailableException("langchain4j call failed: " + e.getMessage(), e);
        }
    }

    private ChatModel openAiModel() {
        String apiKey = envOr("OPENAI_API_KEY", null);
        if (apiKey == null || "disabled".equals(apiKey)) {
            throw new ProcessorUnavailableException("OPENAI_API_KEY not configured for langchain4j mode");
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(envOr("OPENAI_BASE_URL", "https://api.openai.com"))
                .modelName(envOr("OPENAI_CHAT_MODEL", "gpt-4o-mini"))
                .temperature(0.0)
                .timeout(Duration.ofSeconds(45))
                .build();
    }

    static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
