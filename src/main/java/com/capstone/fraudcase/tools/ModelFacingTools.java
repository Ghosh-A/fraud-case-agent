package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.tools.ToolResults.AuditEventResult;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignalsResult;
import com.capstone.fraudcase.tools.ToolResults.LocationCompareResult;
import com.capstone.fraudcase.tools.ToolResults.MerchantRiskResult;
import com.capstone.fraudcase.tools.ToolResults.PolicySearchResult;
import com.capstone.fraudcase.tools.ToolResults.PriorDisputesResult;
import com.capstone.fraudcase.tools.ToolResults.SlaResult;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetailsResult;

/**
 * Allow-listed tools exposed to model clients. Dual-annotated so the SAME
 * object registers with Spring AI and LangChain4j; every invocation runs
 * through {@link ToolExecutor} so model-initiated calls are traced too.
 */
public class ModelFacingTools {

    private final EvidenceTools tools;
    private final ToolExecutor executor;
    private final String caseId;
    private final String maskedCustomerRef;
    private final String transactionRef;

    public ModelFacingTools(EvidenceTools tools, ToolExecutor executor, String caseId,
                            String maskedCustomerRef, String transactionRef) {
        this.tools = tools;
        this.executor = executor;
        this.caseId = caseId;
        this.maskedCustomerRef = maskedCustomerRef;
        this.transactionRef = transactionRef;
    }

    public static ModelFacingTools forCase(EvidenceTools tools, ToolExecutor executor, String caseId,
                                           String maskedCustomerRef, String transactionRef) {
        return new ModelFacingTools(tools, executor, caseId, maskedCustomerRef, transactionRef);
    }

    @dev.langchain4j.agent.tool.Tool("Retrieve transaction state, channel and authentication outcome for the disputed transaction.")
    @org.springframework.ai.tool.annotation.Tool(description = "Retrieve transaction state, channel and authentication outcome for the disputed transaction.")
    public TransactionDetailsResult getTransactionDetails() {
        return executor.execute(caseId, "getTransactionDetails", transactionRef,
                () -> tools.getTransactionDetails(transactionRef, maskedCustomerRef),
                () -> TransactionDetailsResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Check whether the customer device is known and trusted.")
    @org.springframework.ai.tool.annotation.Tool(description = "Check whether the customer device is known and trusted.")
    public DeviceSignalsResult getDeviceSignals() {
        return executor.execute(caseId, "getDeviceSignals", maskedCustomerRef,
                () -> tools.getDeviceSignals(maskedCustomerRef, transactionRef),
                () -> DeviceSignalsResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Compare the transaction location with recent trusted customer locations.")
    @org.springframework.ai.tool.annotation.Tool(description = "Compare the transaction location with recent trusted customer locations.")
    public LocationCompareResult compareRecentLocations() {
        return executor.execute(caseId, "compareRecentLocations", maskedCustomerRef,
                () -> tools.compareRecentLocations(maskedCustomerRef, transactionRef),
                () -> LocationCompareResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Return the synthetic risk profile of the masked merchant.")
    @org.springframework.ai.tool.annotation.Tool(description = "Return the synthetic risk profile of the masked merchant.")
    public MerchantRiskResult getMerchantRiskProfile(
            @dev.langchain4j.agent.tool.P("masked merchant descriptor")
            @org.springframework.ai.tool.annotation.ToolParam(description = "masked merchant descriptor")
            String maskedMerchant) {
        return executor.execute(caseId, "getMerchantRiskProfile", maskedMerchant,
                () -> tools.getMerchantRiskProfile(maskedMerchant, transactionRef),
                () -> MerchantRiskResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Summarise earlier disputes for the masked customer.")
    @org.springframework.ai.tool.annotation.Tool(description = "Summarise earlier disputes for the masked customer.")
    public PriorDisputesResult getPriorDisputes() {
        return executor.execute(caseId, "getPriorDisputes", maskedCustomerRef,
                () -> tools.getPriorDisputes(maskedCustomerRef, transactionRef),
                () -> PriorDisputesResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Calculate the training SLA target for a proposed review route.")
    @org.springframework.ai.tool.annotation.Tool(description = "Calculate the training SLA target for a proposed review route.")
    public SlaResult calculateCaseSla(
            @dev.langchain4j.agent.tool.P("proposed route enum")
            @org.springframework.ai.tool.annotation.ToolParam(description = "proposed route enum")
            String proposedRoute) {
        return executor.execute(caseId, "calculateCaseSla", proposedRoute,
                () -> tools.calculateCaseSla(proposedRoute),
                () -> SlaResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Search approved policy passages. Returns citation metadata or an empty list when nothing meets the threshold.")
    @org.springframework.ai.tool.annotation.Tool(description = "Search approved policy passages. Returns citation metadata or an empty list when nothing meets the threshold.")
    public PolicySearchResult retrievePolicyEvidence(
            @dev.langchain4j.agent.tool.P("policy search query")
            @org.springframework.ai.tool.annotation.ToolParam(description = "policy search query")
            String query) {
        return executor.execute(caseId, "retrievePolicyEvidence", query,
                () -> tools.retrievePolicyEvidence(query),
                () -> PolicySearchResult.unavailable("not executed"));
    }

    @dev.langchain4j.agent.tool.Tool("Append an important audit event for this case.")
    @org.springframework.ai.tool.annotation.Tool(description = "Append an important audit event for this case.")
    public AuditEventResult saveAuditEvent(
            @dev.langchain4j.agent.tool.P("audit event type")
            @org.springframework.ai.tool.annotation.ToolParam(description = "audit event type")
            String eventType,
            @dev.langchain4j.agent.tool.P("short summary")
            @org.springframework.ai.tool.annotation.ToolParam(description = "short summary")
            String summary) {
        return executor.execute(caseId, "saveAuditEvent", eventType + "|" + summary,
                () -> tools.saveAuditEvent(caseId, eventType, summary),
                () -> AuditEventResult.unavailable("not executed"));
    }
}
