package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.AuditEventType;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.AuditEventWriter;
import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import com.capstone.fraudcase.tools.ToolResults.AuditEventResult;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignalsResult;
import com.capstone.fraudcase.tools.ToolResults.LocationCompareResult;
import com.capstone.fraudcase.tools.ToolResults.MerchantRiskResult;
import com.capstone.fraudcase.tools.ToolResults.PolicySearchResult;
import com.capstone.fraudcase.tools.ToolResults.PriorDisputesResult;
import com.capstone.fraudcase.tools.ToolResults.SlaResult;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetailsResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The approved read-only tool catalogue. Pure deterministic logic — tracing,
 * timeouts and argument hashing live in {@link ToolExecutor}.
 */
@Component
public class EvidenceTools {

    private final DeterministicFixtureGenerator fixtures;
    private final PolicyRetriever policyRetriever;
    private final AuditEventWriter auditWriter;

    public EvidenceTools(DeterministicFixtureGenerator fixtures,
                         PolicyRetriever policyRetriever,
                         AuditEventWriter auditWriter) {
        this.fixtures = fixtures;
        this.policyRetriever = policyRetriever;
        this.auditWriter = auditWriter;
    }

    public TransactionDetailsResult getTransactionDetails(String transactionRef, String maskedCustomerRef) {
        FixtureProfile p = fixtures.profileFor(maskedCustomerRef, transactionRef);
        if (!p.transactionFound) {
            return TransactionDetailsResult.notFound(transactionRef);
        }
        TransactionDetailsResult r = new TransactionDetailsResult();
        r.setFound(true);
        r.setTxnStatus(p.txnStatus);
        r.setTimestamp(Instant.now().minus(26, ChronoUnit.HOURS).toString());
        r.setAmount("POSTED-AMOUNT");
        r.setChannel(p.txnChannel);
        r.setAuthOutcome(p.authOutcome);
        r.setMaskedMerchant("MERCHANT ••••" + suffix(transactionRef));
        r.setDuplicateFingerprint(p.duplicateFingerprint);
        return r;
    }

    public DeviceSignalsResult getDeviceSignals(String maskedCustomerRef, String transactionRef) {
        FixtureProfile p = fixtures.profileFor(maskedCustomerRef, transactionRef);
        DeviceSignalsResult r = new DeviceSignalsResult();
        r.setDeviceKnown(p.deviceKnown);
        r.setFirstSeenAt(Instant.now().minus(p.deviceAgeDays, ChronoUnit.DAYS).toString());
        r.setTrustLevel(p.trustLevel);
        r.setObservations(List.of(
                p.deviceKnown ? "device previously seen for this customer" : "device never seen before",
                "device age days=" + p.deviceAgeDays,
                "trust level=" + p.trustLevel));
        return r;
    }

    public LocationCompareResult compareRecentLocations(String maskedCustomerRef, String transactionRef) {
        FixtureProfile p = fixtures.profileFor(maskedCustomerRef, transactionRef);
        LocationCompareResult r = new LocationCompareResult();
        r.setDistanceKm(p.distanceKm);
        r.setTimeDelta("PT2H");
        r.setAnomalyLevel(p.locationAnomaly);
        r.setObservations(List.of(
                "current=" + p.currentLocation,
                "lastTrusted=" + p.lastTrustedLocation,
                "anomaly=" + p.locationAnomaly));
        return r;
    }

    public MerchantRiskResult getMerchantRiskProfile(String maskedMerchant, String transactionRef) {
        FixtureProfile p = fixtures.profileFor(maskedMerchant, transactionRef);
        MerchantRiskResult r = new MerchantRiskResult();
        r.setRiskBand(p.merchantRiskBand);
        r.setBlocklistMatch(p.blocklistMatch);
        r.setCategory(p.merchantCategory);
        r.setObservations(List.of(
                "category=" + p.merchantCategory,
                "blocklistMatch=" + p.blocklistMatch,
                "riskBand=" + p.merchantRiskBand));
        return r;
    }

    public PriorDisputesResult getPriorDisputes(String maskedCustomerRef, String transactionRef) {
        FixtureProfile p = fixtures.profileFor(maskedCustomerRef, transactionRef);
        PriorDisputesResult r = new PriorDisputesResult();
        r.setCount(p.priorDisputeCount);
        r.setSimilarCount(p.similarDisputeCount);
        r.setLookbackDays(p.lookbackDays);
        r.setObservations(List.of(
                "total prior disputes=" + p.priorDisputeCount,
                "similar disputes=" + p.similarDisputeCount,
                "lookbackDays=" + p.lookbackDays));
        return r;
    }

    public SlaResult calculateCaseSla(String proposedRoute) {
        SlaResult r = new SlaResult();
        switch (proposedRoute == null ? "" : proposedRoute) {
            case "FRAUD_INVESTIGATION" -> {
                r.setQueue("FRAUD-OPS");
                r.setPriority("HIGH");
                r.setTargetAt(Instant.now().plus(24, ChronoUnit.HOURS).toString());
                r.setExplanation("suspected unauthorised use — 24h fraud queue target");
            }
            case "DISPUTE_OPERATIONS" -> {
                r.setQueue("DISPUTES");
                r.setPriority("MEDIUM");
                r.setTargetAt(Instant.now().plus(72, ChronoUnit.HOURS).toString());
                r.setExplanation("merchant dispute — 72h dispute operations target");
            }
            case "PAYMENT_OPERATIONS" -> {
                r.setQueue("PAY-OPS");
                r.setPriority("HIGH");
                r.setTargetAt(Instant.now().plus(48, ChronoUnit.HOURS).toString());
                r.setExplanation("duplicate/processing issue — 48h payment ops target");
            }
            default -> {
                r.setQueue("MANUAL-REVIEW");
                r.setPriority("MEDIUM");
                r.setTargetAt(Instant.now().plus(24, ChronoUnit.HOURS).toString());
                r.setExplanation("default manual review queue — 24h target");
            }
        }
        return r;
    }

    public PolicySearchResult retrievePolicyEvidence(String query) {
        PolicySearchResult r = new PolicySearchResult();
        r.setQuery(query);
        List<PolicyCitationRef> hits = policyRetriever.search(query, 4);
        r.setCitations(hits);
        return r;
    }

    public AuditEventResult saveAuditEvent(String caseId, String eventType, String summary) {
        AuditEventResult r = new AuditEventResult();
        AuditEventType type;
        try {
            type = AuditEventType.valueOf(eventType);
        } catch (Exception e) {
            type = AuditEventType.STATE_CHANGED;
        }
        AuditEventEntity saved = auditWriter.record(caseId, type, ActorType.AGENT, "agent", summary, summary);
        r.setEventId(saved.getEventId());
        r.setEventType(saved.getEventType().name());
        r.setRecordedAt(saved.getRecordedAt().toString());
        return r;
    }

    private static String suffix(String ref) {
        return ref == null || ref.length() < 4 ? "0000" : ref.substring(ref.length() - 4);
    }
}
