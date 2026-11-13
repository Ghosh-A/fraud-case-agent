package com.capstone.fraudcase.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer: validation, authz and safe errors. */
@SpringBootTest(properties = {"capstone.processor-mode=rules",
        "spring.datasource.url=jdbc:h2:mem:webtest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class CaseApiMvcTest {

    @Autowired
    private MockMvc mvc;

    private static final String INTAKE = """
            {"maskedCustomerRef":"CUST-••••-4821","homeRegion":"IN-KA",
             "transactionRef":"TXN-SCN-UNAUTH-050","amount":100.00,"currency":"INR",
             "occurredAt":"2026-11-06T08:42:00+05:30","status":"POSTED",
             "channel":"CARD_NOT_PRESENT","maskedMerchant":"M ••••1",
             "issueType":"OTHER","customerStatement":"test statement",
             "cardPossession":"HAS_CARD","merchantRecognised":false,
             "intakeChannel":"WEB","reportedAt":"2026-11-13T10:00:00Z",
             "syntheticDataConfirmed":true,"reviewRequired":true}
            """;

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(INTAKE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agentCanCreateCase() throws Exception {
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(INTAKE)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").exists())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void missingFieldsReturnValidationErrors() throws Exception {
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maskedCustomerRef\":\"CUST-1\"}")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void reviewRequiresReviewerRole() throws Exception {
        mvc.perform(post("/api/cases/CASE-1/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"CLOSE\",\"reason\":\"x\"}")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownCase404() throws Exception {
        mvc.perform(get("/api/cases/NOPE/evidence")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void auditEndpointRestrictedToAuditor() throws Exception {
        mvc.perform(get("/api/cases/CASE-1/audit")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiCsrfExemptForBasicClients() throws Exception {
        var res = mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(INTAKE)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("agent", "agent123")))
                .andReturn();
        assertThat(res.getResponse().getHeader("X-Correlation-Id")).isNotBlank();
    }
}
