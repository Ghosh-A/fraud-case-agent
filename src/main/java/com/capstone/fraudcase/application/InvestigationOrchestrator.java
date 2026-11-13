package com.capstone.fraudcase.application;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.agent.common.CaseSummary;
import com.capstone.fraudcase.agent.common.CollectedSignals;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.agent.common.RecommendationValidationException;
import com.capstone.fraudcase.common.Ids;
import com.capstone.fraudcase.common.Json;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import com.capstone.fraudcase.domain.CaseNotFoundException;
import com.capstone.fraudcase.domain.CaseState;
import com.capstone.fraudcase.domain.CaseStateMachine;
import com.capstone.fraudcase.domain.EvidenceType;
import com.capstone.fraudcase.domain.Provenance;
import com.capstone.fraudcase.domain.Severity;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.AuditEventWriter;
import com.capstone.fraudcase.persistence.entity.EvidenceItemEntity;
import com.capstone.fraudcase.persistence.entity.FraudCaseEntity;
import com.capstone.fraudcase.persistence.entity.PolicyCitationEntity;
import com.capstone.fraudcase.persistence.entity.RecommendationEntity;
import com.capstone.fraudcase.persistence.entity.TransactionClaimEntity;
import com.capstone.fraudcase.persistence.repository.EvidenceItemRepository;
import com.capstone.fraudcase.persistence.repository.FraudCaseRepository;
import com.capstone.fraudcase.persistence.repository.PolicyCitationRepository;
import com.capstone.fraudcase.persistence.repository.RecommendationRepository;
import com.capstone.fraudcase.persistence.repository.ToolTraceRepository;
import com.capstone.fraudcase.persistence.repository.TransactionClaimRepository;
import com.capstone.fraudcase.tools.EvidenceTools;
import com.capstone.fraudcase.tools.ToolExecutor;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignalsResult;
import com.capstone.fraudcase.tools.ToolResults.LocationCompareResult;
import com.capstone.fraudcase.tools.ToolResults.MerchantRiskResult;
import com.capstone.fraudcase.tools.ToolResults.PolicySearchResult;
import com.capstone.fraudcase.tools.ToolResults.PriorDisputesResult;
import com.capstone.fraudcase.tools.ToolResults.SlaResult;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetailsResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bounded plan → allow-listed tools → policy retrieval → processor → validated
 * recommendation → persisted artifacts + state transition. State changes live
 * here, never in model output.
 */
@Service
public class InvestigationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestrator.class);

    private final FraudCaseRepository cases;
    private final TransactionClaimRepository claims;
    private final EvidenceItemRepository evidence;
    private final RecommendationRepository recommendations;
    private final PolicyCitationRepository citations;
    private final ToolTraceRepository traces;
    private final EvidenceTools tools;
    private final ToolExecutor executor;
    private final PolicyRetriever policyRetriever;
    private final ProcessorRegistry registry;
    private final CaseStateMachine states;
    private final AuditEventWriter audit;
    private final CapstoneProperties props;
    private final MeterRegistry meterRegistry;

    public InvestigationOrchestrator(FraudCaseRepository cases, TransactionClaimRepository claims,
                                     EvidenceItemRepository evidence, RecommendationRepository recommendations,
                                     PolicyCitationRepository citations, ToolTraceRepository traces,
                                     EvidenceTools tools, ToolExecutor executor,
                                     PolicyRetriever policyRetriever, ProcessorRegistry registry,
                                     CaseStateMachine states, AuditEventWriter audit,
                                     CapstoneProperties props, MeterRegistry meterRegistry) {
        this.cases = cases;
        this.claims = claims;
        this.evidence = evidence;
        this.recommendations = recommendations;
        this.citations = citations;
        this.traces = traces;
        this.tools = tools;
        this.executor = executor;
        this.policyRetriever = policyRetriever;
        this.registry = registry;
        this.states = states;
        this.audit = audit;
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public FraudCaseEntity run(String caseId, String actorId) {
        FraudCaseEntity c = cases.findById(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
        TransactionClaimEntity claim = claims.findByCaseId(caseId)
                .orElseThrow(() -> new IllegalStateException("case " + caseId + " has no claim"));
        if (c.getStatus() != CaseState.NEW && c.getStatus() != CaseState.INCOMPLETE) {
            throw new IllegalStateException("case " + caseId + " in state " + c.getStatus()
                    + " cannot be (re-)enriched");
        }
        transition(c, CaseState.ENRICHING, actorId);
        audit.record(caseId, AuditEventType.ENRICHMENT_STARTED, ActorType.USER, actorId,
                caseId, "bounded investigation started");

        CaseSummary summary = toSummary(c, claim);
        FraudCaseContext ctx = new FraudCaseContext(summary, props.getInvestigationGoal());

        // ---- bounded plan: fixed allow-listed tool sequence ------------------
        persistEvidence(caseId, EvidenceType.CUSTOMER_STATEMENT, Severity.INFO,
                "Customer statement", "claim: " + claim.getIssueType()
                        + " — \"" + truncate(claim.getCustomerStatement(), 300) + "\"",
                Provenance.INTAKE, claim.getClaimId());

        TransactionDetailsResult txn = executor.execute(caseId, "getTransactionDetails",
                claim.getTransactionRef(),
                () -> tools.getTransactionDetails(claim.getTransactionRef(), c.getMaskedCustomerRef()),
                () -> TransactionDetailsResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.TRANSACTION,
                !txn.isFound() && txn.isAvailable() ? Severity.MEDIUM : Severity.INFO,
                "Transaction details", txn.summary(), txn.getToolResultId());
        if (!txn.isFound()) {
            ctx.addMissing("transaction details unavailable");
        } else if ("UNAVAILABLE".equals(txn.getAuthOutcome())) {
            ctx.addMissing("authentication challenge outcome");
        }

        DeviceSignalsResult dev = executor.execute(caseId, "getDeviceSignals", c.getMaskedCustomerRef(),
                () -> tools.getDeviceSignals(c.getMaskedCustomerRef(), claim.getTransactionRef()),
                () -> DeviceSignalsResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.DEVICE, deviceSeverity(dev), "Device signals",
                dev.summary(), dev.getToolResultId());
        if (!dev.isAvailable()) {
            ctx.addMissing("device signals unavailable");
        }

        LocationCompareResult loc = executor.execute(caseId, "compareRecentLocations",
                c.getMaskedCustomerRef(),
                () -> tools.compareRecentLocations(c.getMaskedCustomerRef(), claim.getTransactionRef()),
                () -> LocationCompareResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.LOCATION, locationSeverity(loc), "Location comparison",
                loc.summary(), loc.getToolResultId());
        if (!loc.isAvailable()) {
            ctx.addMissing("location comparison unavailable");
        }

        MerchantRiskResult mer = executor.execute(caseId, "getMerchantRiskProfile",
                claim.getMaskedMerchant(),
                () -> tools.getMerchantRiskProfile(claim.getMaskedMerchant(), claim.getTransactionRef()),
                () -> MerchantRiskResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.MERCHANT, merchantSeverity(mer), "Merchant risk profile",
                mer.summary(), mer.getToolResultId());
        if (!mer.isAvailable()) {
            ctx.addMissing("merchant risk profile unavailable");
        }

        PriorDisputesResult priors = executor.execute(caseId, "getPriorDisputes",
                c.getMaskedCustomerRef(),
                () -> tools.getPriorDisputes(c.getMaskedCustomerRef(), claim.getTransactionRef()),
                () -> PriorDisputesResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.PRIOR_DISPUTES, priors.getSimilarCount() > 1
                ? Severity.MEDIUM : Severity.INFO, "Prior disputes", priors.summary(),
                priors.getToolResultId());
        if (!priors.isAvailable()) {
            ctx.addMissing("prior dispute history unavailable");
        }

        ctx.setSignals(toSignals(txn, dev, loc, mer, priors));
        ctx.toolTraceIds().addAll(tracesFor(caseId));

        // ---- retrieve -------------------------------------------------------
        PolicySearchResult policy = executor.execute(caseId, "retrievePolicyEvidence",
                summary.issueType().name(),
                () -> {
                    PolicySearchResult r = new PolicySearchResult();
                    r.setQuery(summary.issueType().name());
                    r.setCitations(policyRetriever.retrieveForCase(summary, ctx.signals()));
                    return r;
                },
                () -> PolicySearchResult.unavailable("not executed"));
        ctx.policyCitations().addAll(policy.getCitations());
        ctx.toolTraceIds().addAll(tracesFor(caseId).stream()
                .filter(t -> !ctx.toolTraceIds().contains(t)).toList());
        for (PolicyCitationRef ref : ctx.policyCitations()) {
            addEvidence(ctx, EvidenceType.POLICY, Severity.INFO,
                    "Policy " + ref.getDocumentId() + " §" + ref.getSection(),
                    "score=" + ref.getScore() + " " + truncate(ref.getPassage(), 160),
                    ref.getChunkId());
        }
        audit.record(caseId, AuditEventType.POLICY_RETRIEVED, ActorType.SYSTEM, "system",
                String.valueOf(ctx.policyCitations().size()),
                ctx.policyCitations().size() + " passage(s) above threshold");

        // ---- assess + recommend ---------------------------------------------
        FraudCaseRecommendation rec;
        String mode;
        try {
            FraudCaseProcessor processor = registry.select();
            long modelStart = System.nanoTime();
            rec = processor.analyze(ctx);
            mode = processor.mode();
            meterRegistry.timer("fraud.processor.latency", "mode", mode)
                    .record(System.nanoTime() - modelStart, TimeUnit.NANOSECONDS);
        } catch (RecommendationValidationException | ProcessorUnavailableException e) {
            audit.record(caseId, AuditEventType.RECOMMENDATION_INVALID, ActorType.SYSTEM, "system",
                    e.getMessage(), "invalid/unavailable processor output — rules fallback");
            rec = registry.rulesFallback().analyze(ctx);
            mode = "rules-fallback";
            meterRegistry.counter("fraud.processor.fallback",
                    "reason", e.getClass().getSimpleName()).increment();
        } catch (RuntimeException e) {
            // Any unexpected processor failure (model outage, parse error) fails safe.
            audit.record(caseId, AuditEventType.RECOMMENDATION_INVALID, ActorType.SYSTEM, "system",
                    e.getClass().getSimpleName(), "processor failure — rules fallback");
            rec = registry.rulesFallback().analyze(ctx);
            mode = "rules-fallback";
            meterRegistry.counter("fraud.processor.fallback",
                    "reason", e.getClass().getSimpleName()).increment();
        }

        // SLA for the proposed route (post-assessment evidence).
        final FraudCaseRecommendation decided = rec;
        SlaResult sla = executor.execute(caseId, "calculateCaseSla",
                decided.getRecommendedRoute().name(),
                () -> tools.calculateCaseSla(decided.getRecommendedRoute().name()),
                () -> SlaResult.unavailable("not executed"));
        addEvidence(ctx, EvidenceType.SLA, Severity.INFO, "SLA target", sla.summary(),
                sla.getToolResultId());
        ctx.toolTraceIds().addAll(tracesFor(caseId).stream()
                .filter(t -> !ctx.toolTraceIds().contains(t)).toList());
        rec.setToolTraceIds(ctx.toolTraceIds());
        rec.setEvidenceIds(ctx.evidenceIds());

        persistRecommendation(rec, mode);
        transition(c, rec.getClassification()
                == com.capstone.fraudcase.domain.Classification.INSUFFICIENT_EVIDENCE
                ? CaseState.INCOMPLETE : CaseState.READY_FOR_REVIEW, actorId);
        audit.record(caseId, AuditEventType.RECOMMENDATION_CREATED, ActorType.AGENT, mode,
                rec.getClassification().name(), rec.getClassification() + " -> "
                + rec.getRecommendedRoute() + " (conf " + rec.getConfidence() + ")");
        return cases.save(c);
    }

    private void addEvidence(FraudCaseContext ctx, EvidenceType type, Severity severity,
                             String title, String summary, String sourceRef) {
        EvidenceItemEntity e = persistEvidence(ctx.caseId(), type, severity, title, summary,
                Provenance.TOOL, sourceRef);
        ctx.addEvidence(e.getEvidenceId(), title + " — " + summary);
    }

    private EvidenceItemEntity persistEvidence(String caseId, EvidenceType type, Severity severity,
                                               String title, String summary,
                                               Provenance provenance, String sourceRef) {
        EvidenceItemEntity e = new EvidenceItemEntity();
        e.setEvidenceId(Ids.evidenceId());
        e.setCaseId(caseId);
        e.setType(type);
        e.setSeverity(severity);
        e.setTitle(title);
        e.setValueSummary(truncate(summary, 1200));
        e.setProvenance(provenance);
        e.setSourceRef(sourceRef);
        e.setObservedAt(Instant.now());
        return evidence.save(e);
    }

    private void persistRecommendation(FraudCaseRecommendation rec, String mode) {
        RecommendationEntity entity = new RecommendationEntity();
        entity.setRecommendationId(Ids.recommendationId());
        entity.setCaseId(rec.getCaseId());
        entity.setMode(mode);
        entity.setClassification(rec.getClassification());
        entity.setRoute(rec.getRecommendedRoute());
        entity.setRiskLevel(rec.getRiskLevel());
        entity.setConfidence(rec.getConfidence());
        entity.setRationale(rec.getRationale());
        entity.setEvidenceIdsJson(Json.write(rec.getEvidenceIds()));
        entity.setMissingInformationJson(Json.write(rec.getMissingInformation()));
        entity.setRuleIdsJson(Json.write(rec.getRuleIds()));
        entity.setToolTraceIdsJson(Json.write(rec.getToolTraceIds()));
        entity.setHumanReviewRequired(rec.isHumanReviewRequired());
        entity.setValid(true);
        entity.setCreatedAt(Instant.now());
        recommendations.save(entity);

        for (PolicyCitationRef ref : rec.getPolicyCitations()) {
            PolicyCitationEntity c = new PolicyCitationEntity();
            c.setCitationId(Ids.citationId());
            c.setRecommendationId(entity.getRecommendationId());
            c.setCaseId(rec.getCaseId());
            c.setDocumentId(ref.getDocumentId());
            c.setVersion(ref.getVersion());
            c.setSection(ref.getSection());
            c.setChunkId(ref.getChunkId());
            c.setScore(ref.getScore());
            c.setPassage(truncate(ref.getPassage(), 2000));
            citations.save(c);
        }
    }

    private List<String> tracesFor(String caseId) {
        return new ArrayList<>(traces.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(t -> t.getTraceId()).toList());
    }

    private void transition(FraudCaseEntity c, CaseState to, String actorId) {
        CaseState from = c.getStatus();
        states.requireTransition(from, to);
        c.setStatus(to);
        c.setUpdatedAt(Instant.now());
        audit.record(c.getCaseId(), AuditEventType.STATE_CHANGED, ActorType.SYSTEM, actorId,
                from + "->" + to, from + " -> " + to);
    }

    private static CollectedSignals toSignals(TransactionDetailsResult txn, DeviceSignalsResult dev,
                                              LocationCompareResult loc, MerchantRiskResult mer,
                                              PriorDisputesResult priors) {
        return new CollectedSignals(
                txn.isFound(),
                txn.isFound() ? txn.getAuthOutcome() : null,
                txn.isFound() && txn.isDuplicateFingerprint(),
                dev.isAvailable(), dev.isDeviceKnown(), dev.getTrustLevel(),
                loc.isAvailable(), loc.getDistanceKm(), loc.getAnomalyLevel(),
                mer.isAvailable(), mer.getRiskBand(), mer.isBlocklistMatch(),
                priors.isAvailable() ? priors.getCount() : 0,
                priors.isAvailable() ? priors.getSimilarCount() : 0);
    }

    private static Severity deviceSeverity(DeviceSignalsResult r) {
        if (!r.isAvailable()) {
            return Severity.INFO;
        }
        return (!r.isDeviceKnown() || "LOW".equals(r.getTrustLevel())) ? Severity.HIGH : Severity.INFO;
    }

    private static Severity locationSeverity(LocationCompareResult r) {
        if (!r.isAvailable()) {
            return Severity.INFO;
        }
        return switch (r.getAnomalyLevel() == null ? "NONE" : r.getAnomalyLevel()) {
            case "HIGH" -> Severity.HIGH;
            case "MEDIUM" -> Severity.MEDIUM;
            default -> Severity.INFO;
        };
    }

    private static Severity merchantSeverity(MerchantRiskResult r) {
        if (!r.isAvailable()) {
            return Severity.INFO;
        }
        if (r.isBlocklistMatch() || "HIGH".equals(r.getRiskBand())) {
            return Severity.HIGH;
        }
        return "MEDIUM".equals(r.getRiskBand()) ? Severity.MEDIUM : Severity.INFO;
    }

    private static CaseSummary toSummary(FraudCaseEntity c, TransactionClaimEntity cl) {
        return new CaseSummary(c.getCaseId(), c.getMaskedCustomerRef(), c.getHomeRegion(),
                cl.getTransactionRef(), cl.getAmount(), cl.getCurrency(), cl.getOccurredAt(),
                cl.getTxnStatus(), cl.getTxnChannel(), cl.getMaskedMerchant(), cl.getIssueType(),
                cl.getCustomerStatement(), cl.getCardPossession(), cl.isMerchantRecognised(),
                c.getReportedAt());
    }

    private static String truncate(String s, int n) {
        return s == null || s.length() <= n ? s : s.substring(0, n);
    }
}
