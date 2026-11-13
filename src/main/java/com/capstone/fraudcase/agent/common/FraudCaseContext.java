package com.capstone.fraudcase.agent.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Everything a processor needs: validated intake, collected tool evidence,
 * retrieved policy citations, observed gaps and the prompt variables.
 */
public class FraudCaseContext {

    private final CaseSummary summary;
    private final String investigationGoal;
    private final List<String> evidenceIds = new ArrayList<>();
    private final List<String> toolEvidenceLines = new ArrayList<>();
    private final List<PolicyCitationRef> policyCitations = new ArrayList<>();
    private final List<String> missingInformation = new ArrayList<>();
    private final List<String> toolTraceIds = new ArrayList<>();
    private CollectedSignals signals = CollectedSignals.empty();
    private String conversationContext = "";

    public FraudCaseContext(CaseSummary summary, String investigationGoal) {
        this.summary = summary;
        this.investigationGoal = investigationGoal;
    }

    public String caseId() {
        return summary.caseId();
    }

    public CaseSummary summary() {
        return summary;
    }

    public String investigationGoal() {
        return investigationGoal;
    }

    public List<String> evidenceIds() {
        return evidenceIds;
    }

    public List<String> toolEvidenceLines() {
        return toolEvidenceLines;
    }

    public List<PolicyCitationRef> policyCitations() {
        return policyCitations;
    }

    public List<String> missingInformation() {
        return missingInformation;
    }

    public List<String> toolTraceIds() {
        return toolTraceIds;
    }

    public CollectedSignals signals() {
        return signals;
    }

    public void setSignals(CollectedSignals signals) {
        this.signals = signals == null ? CollectedSignals.empty() : signals;
    }

    public String conversationContext() {
        return conversationContext;
    }

    public void setConversationContext(String conversationContext) {
        this.conversationContext = conversationContext == null ? "" : conversationContext;
    }

    public void addEvidence(String evidenceId, String line) {
        evidenceIds.add(evidenceId);
        toolEvidenceLines.add(evidenceId + ": " + line);
    }

    public void addMissing(String gap) {
        if (!missingInformation.contains(gap)) {
            missingInformation.add(gap);
        }
    }

    public String policyContextText() {
        if (policyCitations.isEmpty()) {
            return "NO POLICY PASSAGES RETRIEVED — abstain from procedural claims.";
        }
        StringBuilder sb = new StringBuilder();
        for (PolicyCitationRef c : policyCitations) {
            sb.append(String.format("[%s v%s §%s chunk=%s score=%.2f] %s%n",
                    c.getDocumentId(), c.getVersion(), c.getSection(), c.getChunkId(), c.getScore(), c.getPassage()));
        }
        return sb.toString();
    }

    /** Template variables required by the prompt contract. */
    public Map<String, Object> promptVariables() {
        return Map.of(
                "caseSummary", summary.toPromptText(),
                "investigationGoal", investigationGoal,
                "toolEvidence", String.join("\n", toolEvidenceLines),
                "policyContext", policyContextText(),
                "conversationContext", conversationContext,
                "missingInformation", missingInformation.isEmpty()
                        ? "none recorded" : String.join("; ", missingInformation));
    }
}
