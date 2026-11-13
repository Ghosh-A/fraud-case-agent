package com.capstone.fraudcase.api;

import com.capstone.fraudcase.api.dto.CaseIntakeRequest;
import com.capstone.fraudcase.api.dto.FollowUpRequest;
import com.capstone.fraudcase.api.dto.ReviewRequest;
import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.common.Json;
import com.capstone.fraudcase.persistence.entity.AuditEventEntity;
import com.capstone.fraudcase.persistence.entity.EvidenceItemEntity;
import com.capstone.fraudcase.persistence.entity.FraudCaseEntity;
import com.capstone.fraudcase.persistence.entity.PolicyCitationEntity;
import com.capstone.fraudcase.persistence.entity.RecommendationEntity;
import com.capstone.fraudcase.persistence.entity.ReviewDispositionEntity;
import com.capstone.fraudcase.persistence.entity.ToolTraceEntity;
import com.capstone.fraudcase.persistence.entity.TransactionClaimEntity;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseApiController {

    private final FraudCaseService service;

    public CaseApiController(FraudCaseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCase(@Valid @RequestBody CaseIntakeRequest req,
                                                          Authentication auth) {
        FraudCaseEntity c = service.createCase(req, actor(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(caseSummary(c));
    }

    @GetMapping
    public List<Map<String, Object>> listCases() {
        return service.listCases().stream().map(this::caseSummary).toList();
    }

    @GetMapping("/{caseId}")
    public Map<String, Object> getCase(@PathVariable String caseId) {
        FraudCaseEntity c = service.getCase(caseId);
        TransactionClaimEntity cl = service.getClaim(caseId);
        Map<String, Object> out = caseSummary(c);
        out.put("claim", Map.of(
                "claimId", cl.getClaimId(),
                "transactionRef", cl.getTransactionRef(),
                "amount", cl.getAmount(),
                "currency", cl.getCurrency(),
                "occurredAt", cl.getOccurredAt().toString(),
                "status", cl.getTxnStatus().name(),
                "channel", cl.getTxnChannel().name(),
                "maskedMerchant", cl.getMaskedMerchant(),
                "issueType", cl.getIssueType().name(),
                "merchantRecognised", cl.isMerchantRecognised()));
        return out;
    }

    @PostMapping("/{caseId}/investigations")
    public Map<String, Object> investigate(@PathVariable String caseId, Authentication auth) {
        FraudCaseEntity c = service.investigate(caseId, actor(auth));
        Map<String, Object> out = caseSummary(c);
        out.put("toolTraces", service.tracesFor(caseId).size());
        return out;
    }

    @GetMapping("/{caseId}/evidence")
    public List<EvidenceItemEntity> evidence(@PathVariable String caseId) {
        return service.evidenceFor(caseId);
    }

    @GetMapping("/{caseId}/tool-traces")
    public List<ToolTraceEntity> toolTraces(@PathVariable String caseId) {
        return service.tracesFor(caseId);
    }

    @GetMapping("/{caseId}/recommendation")
    public Map<String, Object> recommendation(@PathVariable String caseId) {
        RecommendationEntity r = service.latestRecommendation(caseId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recommendationId", r.getRecommendationId());
        out.put("caseId", r.getCaseId());
        out.put("mode", r.getMode());
        out.put("classification", r.getClassification().name());
        out.put("recommendedRoute", r.getRoute().name());
        out.put("riskLevel", r.getRiskLevel().name());
        out.put("confidence", r.getConfidence());
        out.put("humanReviewRequired", r.isHumanReviewRequired());
        out.put("rationale", r.getRationale());
        out.put("evidenceIds", Json.readStringList(r.getEvidenceIdsJson()));
        out.put("missingInformation", Json.readStringList(r.getMissingInformationJson()));
        out.put("ruleIds", Json.readStringList(r.getRuleIdsJson()));
        out.put("toolTraceIds", Json.readStringList(r.getToolTraceIdsJson()));
        out.put("createdAt", r.getCreatedAt().toString());
        return out;
    }

    @GetMapping("/{caseId}/citations")
    public List<PolicyCitationEntity> citations(@PathVariable String caseId) {
        return service.citationsFor(caseId);
    }

    @GetMapping("/{caseId}/audit")
    public List<AuditEventEntity> audit(@PathVariable String caseId) {
        return service.auditFor(caseId);
    }

    @PostMapping("/{caseId}/messages")
    public Map<String, String> followUp(@PathVariable String caseId,
                                        @Valid @RequestBody FollowUpRequest req,
                                        Authentication auth) {
        return Map.of("caseId", caseId, "answer", service.askFollowUp(caseId, req.getMessage(), actor(auth)));
    }

    @PostMapping("/{caseId}/reviews")
    public ResponseEntity<Map<String, Object>> review(@PathVariable String caseId,
                                                      @Valid @RequestBody ReviewRequest req,
                                                      Authentication auth) {
        ReviewDispositionEntity r = service.submitReview(caseId, req.getDisposition(),
                req.getReason(), req.getChosenRoute(), actor(auth));
        FraudCaseEntity c = service.getCase(caseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "reviewId", r.getReviewId(),
                "caseId", caseId,
                "disposition", r.getDisposition().name(),
                "caseState", c.getStatus().name()));
    }

    private Map<String, Object> caseSummary(FraudCaseEntity c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("caseId", c.getCaseId());
        out.put("maskedCustomerRef", c.getMaskedCustomerRef());
        out.put("homeRegion", c.getHomeRegion());
        out.put("status", c.getStatus().name());
        out.put("intakeChannel", c.getIntakeChannel().name());
        out.put("analystId", c.getAnalystId());
        out.put("reportedAt", c.getReportedAt().toString());
        out.put("createdAt", c.getCreatedAt().toString());
        return out;
    }

    private String actor(Authentication auth) {
        return auth == null ? "anonymous" : auth.getName();
    }
}
