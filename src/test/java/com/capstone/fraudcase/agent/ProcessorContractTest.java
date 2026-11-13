package com.capstone.fraudcase.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.Contexts;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import com.capstone.fraudcase.agent.common.PromptTemplates;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.agent.langchain4j.FraudInvestigationService;
import com.capstone.fraudcase.agent.langchain4j.LangChain4jFraudCaseAgent;
import com.capstone.fraudcase.agent.langchain4j.Lc4jChatMemoryStore;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.agent.springai.SpringAiFraudCaseAgent;
import com.capstone.fraudcase.common.Json;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.AuditEventWriter;
import com.capstone.fraudcase.persistence.repository.ToolTraceRepository;
import com.capstone.fraudcase.tools.EvidenceTools;
import com.capstone.fraudcase.tools.DeterministicFixtureGenerator;
import com.capstone.fraudcase.tools.ToolExecutor;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * Parity: every FraudCaseProcessor implementation must satisfy the same output
 * schema invariants for the same context — regardless of framework.
 */
class ProcessorContractTest {

    private static final String FIXED_JSON = """
            {"caseId":"CASE-TEST-1","classification":"UNAUTHORISED_TRANSACTION",
             "riskLevel":"HIGH","recommendedRoute":"FRAUD_INVESTIGATION",
             "confidence":0.8,"humanReviewRequired":true,
             "evidenceIds":["EV-1","EV-2"],
             "policyCitations":[{"documentId":"POL-CNP-017","version":"3.2",
                "section":"3.2","chunkId":"pol-cnp-017-c1","score":0.8}],
             "missingInformation":["authentication challenge outcome"],
             "ruleIds":[],"toolTraceIds":["TR-1"],
             "rationale":"New device + impossible travel on denied merchant."}
            """;

    private final RecommendationValidator validator = new RecommendationValidator();
    private final PromptTemplates prompts = new PromptTemplates();
    private final CapstoneProperties props = new CapstoneProperties();
    private final EvidenceTools tools = new EvidenceTools(
            new DeterministicFixtureGenerator(props),
            mock(PolicyRetriever.class), mock(AuditEventWriter.class));
    private final ToolExecutor executor = new ToolExecutor(mockTraces(), props);
    private final ChatMemory memory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(12).build();

    private ToolTraceRepository mockTraces() {
        ToolTraceRepository r = mock(ToolTraceRepository.class);
        when(r.save(any())).thenAnswer(i -> i.getArgument(0));
        return r;
    }

    private FraudCaseProcessor rules() {
        return new RulesFraudCaseAgent(validator);
    }

    private FraudCaseProcessor springAi() {
        ChatModel stub = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(FIXED_JSON))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }

            @Override
            public ChatOptions getDefaultOptions() {
                return new DefaultChatOptions();
            }
        };
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(stub);
        return new SpringAiFraudCaseAgent(provider, memory, prompts, validator, tools, executor, props);
    }

    private FraudCaseProcessor langchain4j() {
        FraudCaseRecommendation fixed = Json.read(FIXED_JSON, FraudCaseRecommendation.class);
        FraudInvestigationService stub = new FraudInvestigationService() {
            @Override
            public FraudCaseRecommendation analyze(String caseId, String schema, String userPrompt) {
                FraudCaseRecommendation r = Json.read(FIXED_JSON, FraudCaseRecommendation.class);
                r.setCaseId(caseId);
                return r;
            }
        };
        LangChain4jFraudCaseAgent agent = new LangChain4jFraudCaseAgent(
                prompts, validator, tools, executor, mock(Lc4jChatMemoryStore.class), props);
        agent.setServiceForTesting(stub);
        return agent;
    }

    static Stream<String> allModes() {
        return Stream.of("rules", "spring-ai", "langchain4j");
    }

    private FraudCaseProcessor byMode(String mode) {
        return switch (mode) {
            case "rules" -> rules();
            case "spring-ai" -> springAi();
            default -> langchain4j();
        };
    }

    @ParameterizedTest(name = "{0} returns the agreed schema")
    @MethodSource("allModes")
    void allProcessorsSatisfyContract(String mode) {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation r = byMode(mode).analyze(ctx);
        assertThat(r.getCaseId()).isEqualTo(ctx.caseId());
        assertThat(r.getClassification()).isNotNull();
        assertThat(r.getRecommendedRoute()).isNotNull();
        assertThat(r.getRiskLevel()).isNotNull();
        assertThat(r.getConfidence()).isBetween(0.0, 1.0);
        assertThat(r.isHumanReviewRequired()).isTrue();
        assertThat(r.getEvidenceIds()).isNotEmpty();
        assertThat(r.getRationale()).isNotBlank();
        assertThat(r.getPolicyCitations()).allSatisfy(c -> {
            assertThat(c.getDocumentId()).isNotBlank();
            assertThat(c.getVersion()).isNotBlank();
            assertThat(c.getChunkId()).isNotBlank();
        });
    }

    @Test
    void bothFrameworksAgreeWithRulesOnSharedFixtures() {
        FraudCaseContext ctx = Contexts.unauthCase();
        FraudCaseRecommendation rulesRec = rules().analyze(ctx);
        FraudCaseRecommendation spring = springAi().analyze(ctx);
        FraudCaseRecommendation lc4j = langchain4j().analyze(ctx);
        assertThat(spring.getClassification()).isEqualTo(rulesRec.getClassification());
        assertThat(lc4j.getClassification()).isEqualTo(rulesRec.getClassification());
        assertThat(spring.getRecommendedRoute()).isEqualTo(rulesRec.getRecommendedRoute());
        assertThat(lc4j.getRecommendedRoute()).isEqualTo(rulesRec.getRecommendedRoute());
    }

    @Test
    void springAiWithoutModelFailsSafely() {
        ObjectProvider<ChatModel> empty = mock(ObjectProvider.class);
        when(empty.getIfAvailable()).thenReturn(null);
        FraudCaseProcessor agent = new SpringAiFraudCaseAgent(
                empty, memory, prompts, validator, tools, executor, props);
        assertThatThrownBy(() -> agent.analyze(Contexts.unauthCase()))
                .isInstanceOf(ProcessorUnavailableException.class);
    }
}
