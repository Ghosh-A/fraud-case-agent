package com.capstone.fraudcase.api;

import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.common.Json;
import com.capstone.fraudcase.domain.CaseNotFoundException;
import com.capstone.fraudcase.domain.CaseState;
import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.persistence.entity.FraudCaseEntity;
import com.capstone.fraudcase.persistence.entity.RecommendationEntity;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UiController {

    private final FraudCaseService service;

    public UiController(FraudCaseService service) {
        this.service = service;
    }

    @GetMapping({"/", "/cases"})
    public String caseList(Model model) {
        model.addAttribute("cases", service.listCases());
        return "cases";
    }

    @GetMapping("/cases/new")
    public String intakeForm() {
        return "intake";
    }

    @PostMapping("/cases")
    public String submitIntake(com.capstone.fraudcase.api.dto.CaseIntakeRequest req,
                               Authentication auth, RedirectAttributes ra, Model model) {
        try {
            FraudCaseEntity c = service.createCase(req, actor(auth));
            ra.addFlashAttribute("notice", "Case " + c.getCaseId() + " created");
            return "redirect:/cases/" + c.getCaseId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "intake";
        }
    }

    @GetMapping("/cases/{caseId}")
    public String workspace(@PathVariable String caseId, Model model) {
        loadCase(model, caseId);
        model.addAttribute("traces", service.tracesFor(caseId));
        return "investigation";
    }

    @PostMapping("/cases/{caseId}/investigate")
    public String investigate(@PathVariable String caseId, Authentication auth, RedirectAttributes ra) {
        try {
            service.investigate(caseId, actor(auth));
            ra.addFlashAttribute("notice", "Investigation complete");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Investigation failed safely: " + e.getMessage());
        }
        return "redirect:/cases/" + caseId;
    }

    @GetMapping("/cases/{caseId}/evidence")
    public String evidence(@PathVariable String caseId, Model model) {
        loadCase(model, caseId);
        model.addAttribute("evidence", service.evidenceFor(caseId));
        return "evidence";
    }

    @GetMapping("/cases/{caseId}/recommendation")
    public String recommendation(@PathVariable String caseId, Model model) {
        loadCase(model, caseId);
        RecommendationEntity rec = service.latestRecommendation(caseId);
        model.addAttribute("rec", rec);
        model.addAttribute("evidenceIds", Json.readStringList(rec.getEvidenceIdsJson()));
        model.addAttribute("missingInfo", Json.readStringList(rec.getMissingInformationJson()));
        model.addAttribute("ruleIds", Json.readStringList(rec.getRuleIdsJson()));
        model.addAttribute("traceIds", Json.readStringList(rec.getToolTraceIdsJson()));
        return "recommendation";
    }

    @GetMapping("/cases/{caseId}/citations")
    public String citations(@PathVariable String caseId, Model model) {
        loadCase(model, caseId);
        model.addAttribute("citations", service.citationsFor(caseId));
        return "citations";
    }

    @GetMapping("/cases/{caseId}/review")
    public String review(@PathVariable String caseId, Model model) {
        loadCase(model, caseId);
        try {
            model.addAttribute("rec", service.latestRecommendation(caseId));
        } catch (IllegalStateException e) {
            model.addAttribute("rec", null);
        }
        model.addAttribute("dispositions", service.allowedDispositions(caseId));
        model.addAttribute("routes", Route.values());
        model.addAttribute("reviews", service.reviewsFor(caseId));
        model.addAttribute("audit", service.auditFor(caseId));
        return "review";
    }

    @PostMapping("/cases/{caseId}/review")
    public String submitReview(@PathVariable String caseId, @RequestParam Disposition disposition,
                               @RequestParam(required = false) String reason,
                               @RequestParam(required = false) Route chosenRoute,
                               @RequestParam(required = false) boolean acknowledgedMissing,
                               Authentication auth, RedirectAttributes ra) {
        try {
            service.submitReview(caseId, disposition, reason, chosenRoute, actor(auth));
            ra.addFlashAttribute("notice", "Disposition " + disposition + " recorded");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cases/" + caseId + "/review";
    }

    @PostMapping("/cases/{caseId}/message")
    public String followUp(@PathVariable String caseId, @RequestParam String message,
                           Authentication auth, RedirectAttributes ra) {
        try {
            String answer = service.askFollowUp(caseId, message, actor(auth));
            ra.addFlashAttribute("followUp", answer);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Follow-up failed safely");
        }
        return "redirect:/cases/" + caseId;
    }

    private void loadCase(Model model, String caseId) {
        FraudCaseEntity c = service.getCase(caseId);
        if (c == null) {
            throw new CaseNotFoundException(caseId);
        }
        model.addAttribute("case", c);
        try {
            model.addAttribute("claim", service.getClaim(caseId));
        } catch (CaseNotFoundException e) {
            model.addAttribute("claim", null);
        }
        model.addAttribute("states", CaseState.values());
    }

    private String actor(Authentication auth) {
        return auth == null ? "anonymous" : auth.getName();
    }

    @GetMapping("/audit")
    public String auditOverview(Model model, @RequestParam(required = false) String caseId) {
        model.addAttribute("cases", service.listCases());
        if (caseId != null) {
            model.addAttribute("caseId", caseId);
            model.addAttribute("events", service.auditFor(caseId));
        }
        return "audit";
    }
}
