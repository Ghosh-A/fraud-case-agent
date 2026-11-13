package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.CaseSummary;
import com.capstone.fraudcase.agent.common.FollowUpAssistant;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.api.dto.CaseIntakeRequest;
import com.capstone.fraudcase.common.Ids;
import com.capstone.fraudcase.common.Json;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import com.capstone.fraudcase.domain.CaseNotFoundException;
import com.capstone.fraudcase.domain.CaseState;
import com.capstone.fraudcase.domain.CaseStateMachine;
import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.MaskingValidator;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.persistence.AuditEventWriter;
import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import com.capstone.fraudcase.persistence.entity.EvidenceItemEntity;
import com.capstone.fraudcase.persistence.entity.FraudCaseEntity;
import com.capstone.fraudcase.persistence.entity.PolicyCitationEntity;
import com.capstone.fraudcase.persistence.entity.RecommendationEntity;
import com.capstone.fraudcase.persistence.entity.ReviewDispositionEntity;
import com.capstone.fraudcase.persistence.entity.ToolTraceEntity;
import com.capstone.fraudcase.persistence.entity.TransactionClaimEntity;
import com.capstone.fraudcase.persistence.repository.AuditEventRepository;
import com.capstone.fraudcase.persistence.repository.EvidenceItemRepository;
import com.capstone.fraudcase.persistence.repository.FraudCaseRepository;
import com.capstone.fraudcase.persistence.repository.PolicyCitationRepository;
import com.capstone.fraudcase.persistence.repository.RecommendationRepository;
import com.capstone.fraudcase.persistence.repository.ReviewDispositionRepository;
import com.capstone.fraudcase.persistence.repository.ToolTraceRepository;
import com.capstone.fraudcase.persistence.repository.TransactionClaimRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudCaseService {

    private final FraudCaseRepository cases;
    private final TransactionClaimRepository claims;
    private final EvidenceItemRepository evidence;
    private final RecommendationRepository recommendations;
    private final PolicyCitationRepository citations;
    private final ToolTraceRepository traces;
    private final ReviewDispositionRepository reviews;
    private final AuditEventRepository auditEvents;
    private final AuditEventWriter audit;
    private final MaskingValidator masking;
    private final CaseStateMachine states;
    private final InvestigationOrchestrator orchestrator;
    private final CaseConversationMemory memory;
    private final ProcessorRegistry registry;
    private final List<FollowUpAssistant> assistants;
    private final com.capstone.fraudcase.knowledge.PolicyRetriever policyRetriever;

    public FraudCaseService(FraudCaseRepository cases, TransactionClaimRepository claims,
                            EvidenceItemRepository evidence, RecommendationRepository recommendations,
                            PolicyCitationRepository citations, ToolTraceRepository traces,
                            ReviewDispositionRepository reviews, AuditEventRepository auditEvents,
                            AuditEventWriter audit, MaskingValidator masking, CaseStateMachine states,
                            InvestigationOrchestrator orchestrator, CaseConversationMemory memory,
                            ProcessorRegistry registry, List<FollowUpAssistant> assistants,
                            com.capstone.fraudcase.knowledge.PolicyRetriever policyRetriever) {
        this.cases = cases;
        this.claims = claims;
        this.evidence = evidence;
        this.recommendations = recommendations;
        this.citations = citations;
        this.traces = traces;
        this.reviews = reviews;
        this.auditEvents = auditEvents;
        this.audit = audit;
        this.masking = masking;
        this.states = states;
        this.orchestrator = orchestrator;
        this.memory = memory;
        this.registry = registry;
        this.assistants = assistants;
        this.policyRetriever = policyRetriever;
    }

    @Transactional
    public FraudCaseEntity createCase(CaseIntakeRequest req, String actorId) {
        if (!masking.isMaskedReference(req.getMaskedCustomerRef())
                || !masking.isMaskedReference(req.getTransactionRef())) {
            throw new IllegalArgumentException("unmasked identifier rejected — use masked/tokenised refs");
        }
        if (masking.containsUnmaskedIdentifier(req.getMaskedMerchant())
                || masking.containsUnmaskedIdentifier(req.getCustomerStatement())) {
            throw new IllegalArgumentException("input appears to contain an unmasked identifier");
        }
        if (masking.containsMarkup(req.getCustomerStatement())) {
            throw new IllegalArgumentException("customer statement must be plain text");
        }
        Instant now = Instant.now();
        FraudCaseEntity c = new FraudCaseEntity();
        // Guard against ever merging onto an existing row — ids are unique by
        // construction, but a collision must never silently overwrite a case.
        String caseId;
        do {
            caseId = Ids.caseId();
        } while (cases.existsById(caseId));
        c.setCaseId(caseId);
        c.setMaskedCustomerRef(req.getMaskedCustomerRef());
        c.setHomeRegion(req.getHomeRegion());
        c.setStatus(CaseState.NEW);
        c.setIntakeChannel(req.getIntakeChannel());
        c.setAnalystId(actorId);
        c.setCreatedBy(actorId);
        c.setReportedAt(req.getReportedAt() != null ? req.getReportedAt() : now);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        cases.save(c);

        TransactionClaimEntity cl = new TransactionClaimEntity();
        cl.setClaimId(Ids.claimId());
        cl.setCaseId(c.getCaseId());
        cl.setTransactionRef(req.getTransactionRef());
        cl.setAmount(req.getAmount());
        cl.setCurrency(req.getCurrency());
        cl.setOccurredAt(req.getOccurredAt());
        cl.setTxnStatus(req.getStatus());
        cl.setTxnChannel(req.getChannel());
        cl.setMaskedMerchant(req.getMaskedMerchant());
        cl.setIssueType(req.getIssueType());
        cl.setCustomerStatement(req.getCustomerStatement());
        cl.setCardPossession(req.getCardPossession());
        cl.setMerchantRecognised(Boolean.TRUE.equals(req.getMerchantRecognised()));
        claims.save(cl);

        audit.record(c.getCaseId(), AuditEventType.CASE_CREATED, ActorType.USER, actorId,
                c.getCaseId(), "case created via " + req.getIntakeChannel());
        return c;
    }

    public FraudCaseEntity getCase(String caseId) {
        return cases.findById(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    public TransactionClaimEntity getClaim(String caseId) {
        return claims.findByCaseId(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    public List<FraudCaseEntity> listCases() {
        return cases.findAll();
    }

    public FraudCaseEntity investigate(String caseId, String actorId) {
        return orchestrator.run(caseId, actorId);
    }

    public List<EvidenceItemEntity> evidenceFor(String caseId) {
        getCase(caseId);
        return evidence.findByCaseIdOrderByObservedAtAsc(caseId);
    }

    public List<ToolTraceEntity> tracesFor(String caseId) {
        getCase(caseId);
        return traces.findByCaseIdOrderByCreatedAtAsc(caseId);
    }

    public RecommendationEntity latestRecommendation(String caseId) {
        getCase(caseId);
        return recommendations.findTopByCaseIdAndValidTrueOrderByCreatedAtDesc(caseId)
                .orElseThrow(() -> new IllegalStateException("no validated recommendation for " + caseId));
    }

    public List<PolicyCitationEntity> citationsFor(String caseId) {
        getCase(caseId);
        RecommendationEntity rec = latestRecommendation(caseId);
        return citations.findByRecommendationId(rec.getRecommendationId());
    }

    public List<ReviewDispositionEntity> reviewsFor(String caseId) {
        getCase(caseId);
        return reviews.findByCaseIdOrderByRecordedAtDesc(caseId);
    }

    public List<AuditEventEntity> auditFor(String caseId) {
        getCase(caseId);
        return auditEvents.findByCaseIdOrderByRecordedAtAsc(caseId);
    }

    @Transactional
    public ReviewDispositionEntity submitReview(String caseId, Disposition disposition,
                                                String reason, Route chosenRoute, String reviewerId) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reviewer rationale is mandatory");
        }
        FraudCaseEntity c = getCase(caseId);
        if (c.getStatus() == CaseState.READY_FOR_REVIEW) {
            transition(c, CaseState.UNDER_REVIEW, reviewerId);
        } else if (c.getStatus() != CaseState.UNDER_REVIEW && c.getStatus() != CaseState.INCOMPLETE) {
            throw new IllegalStateException(
                    "case " + caseId + " is not awaiting review (state=" + c.getStatus() + ")");
        }
        CaseState target = targetFor(disposition);
        ReviewDispositionEntity r = new ReviewDispositionEntity();
        r.setReviewId(Ids.reviewId());
        r.setCaseId(caseId);
        r.setReviewerId(reviewerId);
        r.setDisposition(disposition);
        r.setReason(reason);
        r.setChosenRoute(chosenRoute == null ? null : chosenRoute.name());
        r.setRecordedAt(Instant.now());
        reviews.save(r);
        if (target != c.getStatus()) {
            transition(c, target, reviewerId);
        }
        cases.save(c);
        audit.record(caseId, AuditEventType.REVIEW_RECORDED, ActorType.REVIEWER, reviewerId,
                disposition + "|" + (chosenRoute == null ? "-" : chosenRoute),
                disposition + " by " + reviewerId + " -> " + target);
        return r;
    }

    /** Case-scoped follow-up — memory is read and written under this caseId only. */
    @Transactional
    public String askFollowUp(String caseId, String question, String actorId) {
        FraudCaseEntity c = getCase(caseId);
        TransactionClaimEntity cl = getClaim(caseId);
        memory.append(caseId, "user", question);
        audit.record(caseId, AuditEventType.MESSAGE_ADDED, ActorType.USER, actorId,
                question, "follow-up question received");

        CaseSummary summary = new CaseSummary(c.getCaseId(), c.getMaskedCustomerRef(), c.getHomeRegion(),
                cl.getTransactionRef(), cl.getAmount(), cl.getCurrency(), cl.getOccurredAt(),
                cl.getTxnStatus(), cl.getTxnChannel(), cl.getMaskedMerchant(), cl.getIssueType(),
                cl.getCustomerStatement(), cl.getCardPossession(), cl.isMerchantRecognised(),
                c.getReportedAt());
        FraudCaseContext ctx = new FraudCaseContext(summary, "Answer the follow-up question "
                + "using only this case's evidence and policy citations.");
        for (EvidenceItemEntity e : evidenceFor(caseId)) {
            ctx.addEvidence(e.getEvidenceId(), e.getTitle() + " — " + e.getValueSummary());
        }
        ctx.setConversationContext(memory.renderForPrompt(caseId));
        try {
            RecommendationEntity rec = latestRecommendation(caseId);
            for (PolicyCitationEntity pc : citations.findByRecommendationId(rec.getRecommendationId())) {
                ctx.policyCitations().add(new PolicyCitationRef(pc.getDocumentId(), pc.getVersion(),
                        pc.getSection(), pc.getChunkId(), pc.getScore(), pc.getPassage()));
            }
        } catch (IllegalStateException ignored) {
            ctx.policyCitations().addAll(policyRetriever.retrieveForCase(summary, null));
        }

        String answer;
        FollowUpAssistant assistant = assistants.stream()
                .filter(a -> a.mode() != null && a.mode().equals(registry.configuredMode()))
                .findFirst()
                .orElseGet(() -> assistants.stream()
                        .filter(a -> "rules".equals(a.mode())).findFirst().orElseThrow());
        try {
            answer = assistant.reply(ctx, question);
        } catch (RuntimeException e) {
            answer = assistants.stream().filter(a -> "rules".equals(a.mode()))
                    .findFirst().orElseThrow().reply(ctx, question);
        }
        memory.append(caseId, "assistant", answer);
        audit.record(caseId, AuditEventType.FOLLOW_UP_ANSWERED, ActorType.AGENT, assistant.mode(),
                question, "follow-up answered");
        return answer;
    }

    /** Dispositions the state machine permits from the case's current state. */
    public List<Disposition> allowedDispositions(String caseId) {
        CaseState s = getCase(caseId).getStatus();
        if (!List.of(CaseState.READY_FOR_REVIEW, CaseState.UNDER_REVIEW, CaseState.INCOMPLETE)
                .contains(s)) {
            return List.of();
        }
        // READY_FOR_REVIEW promotes to UNDER_REVIEW before the disposition lands.
        CaseState eff = s == CaseState.READY_FOR_REVIEW ? CaseState.UNDER_REVIEW : s;
        return java.util.Arrays.stream(Disposition.values())
                .filter(d -> {
                    CaseState t = targetFor(d);
                    return t == eff || states.canTransition(eff, t);
                })
                .toList();
    }

    private static CaseState targetFor(Disposition disposition) {
        return switch (disposition) {
            case ACCEPT_ROUTE, ESCALATE, REROUTE, OVERRIDE -> CaseState.ROUTED;
            case REQUEST_INFORMATION -> CaseState.INCOMPLETE;
            case CLOSE -> CaseState.CLOSED;
        };
    }

    private void transition(FraudCaseEntity c, CaseState to, String actorId) {
        states.requireTransition(c.getStatus(), to);
        c.setStatus(to);
        c.setUpdatedAt(Instant.now());
    }
}
