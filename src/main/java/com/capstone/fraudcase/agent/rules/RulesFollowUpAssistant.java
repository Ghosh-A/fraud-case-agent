package com.capstone.fraudcase.agent.rules;

import com.capstone.fraudcase.agent.common.FollowUpAssistant;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Deterministic follow-up answers for rules mode and as a safe fallback. */
@Component
public class RulesFollowUpAssistant implements FollowUpAssistant {

    @Override
    public String mode() {
        return "rules";
    }

    @Override
    public String reply(FraudCaseContext context, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question for ").append(context.caseId()).append(": ").append(question).append("\n\n");
        sb.append("Evidence on file:\n");
        if (context.toolEvidenceLines().isEmpty()) {
            sb.append("- none collected yet\n");
        } else {
            context.toolEvidenceLines().forEach(l -> sb.append("- ").append(l).append('\n'));
        }
        if (!context.policyCitations().isEmpty()) {
            sb.append("Policy citations:\n");
            sb.append(context.policyCitations().stream()
                    .map(c -> "- " + c.getDocumentId() + " v" + c.getVersion() + " §" + c.getSection()
                            + " (chunk " + c.getChunkId() + ", score " + c.getScore() + ")")
                    .collect(Collectors.joining("\n"))).append('\n');
        } else {
            sb.append("Policy citations: none above threshold — do not assume a procedure.\n");
        }
        if (!context.missingInformation().isEmpty()) {
            sb.append("Still missing: ").append(String.join("; ", context.missingInformation())).append('\n');
        }
        sb.append("\nNote: rules mode cannot interpret new facts; this summary reflects recorded evidence only.");
        return sb.toString();
    }
}
