# Capstone Self-Evaluation Report

**Participant:** Angel Ghosh
**Project:** Intelligent Payment Fraud Investigation and Dispute Resolution Agent (`fraud-case-agent`)
**Date:** 2026-11-13
**Evaluator role:** Self-evaluation as senior Java AI architect, BFS fraud-domain reviewer, secure-code reviewer, and test architect — per `Devin_Self_Evaluation_Prompt_Fraud_Capstone.md`

## 1. Executive Summary

- **Project:** Spring Boot service that intakes synthetic payment-fraud/dispute cases, runs a bounded plan of deterministic evidence tools, retrieves policy via local RAG, emits a schema-validated routing recommendation, and requires human review for every disposition.
- **Primary framework:** Spring AI 1.1.8
- **Secondary framework:** LangChain4j 1.22.0 (plus a deterministic `rules` processor — all three behind the `FraudCaseProcessor` contract)
- **Build status:** PASS — `mvn clean verify` green, `fraud-case-agent-1.0.0.jar` produced
- **Test status:** 70 tests, 0 failures, 0 errors, 0 skipped — all offline, no credentials
- **Overall score:** 83/100
- **Percentage:** 83%
- **Readiness:** Demo Ready
- **Scoring model used:** Both frameworks implemented → metrics 4 and 5 scored at 10 points each, plus framework-parity review (§6).

## 2. Repository and Technology Overview

| Area | Finding | Evidence |
|---|---|---|
| App name / objective | `fraud-case-agent` — explainable triage assistant for fraud/dispute cases; recommends only, humans decide | `README.md`, `pom.xml` `artifactId` |
| Java / Spring Boot | Java 21, Spring Boot 3.5.16 | `pom.xml` |
| Build tool | Maven 3.8.7 (no wrapper) | `pom.xml` |
| AI frameworks | **Both** Spring AI 1.1.8 (`spring-ai-bom`, `spring-ai-starter-model-openai`) and LangChain4j 1.22.0 | `pom.xml`, `agent/springai/`, `agent/langchain4j/` |
| Model provider | OpenAI-compatible, env-configured (`OPENAI_API_KEY`/`BASE_URL`/`CHAT_MODEL`); disabled by default (`spring.ai.model.chat: none`) | `application.yml`, `application-example.yml` |
| Database | MySQL 8 + Flyway (`V1__init.sql`) + Spring Data JPA; 9 tables | `application.yml`, `db/migration/` |
| Vector store | Custom `InMemoryPolicyVectorStore` + `LocalHashEmbeddingModel` (deterministic, offline, seed `fraud-training-v1`) | `knowledge/` |
| UI | Thymeleaf + Spring Security, 6+ screens | `templates/`, `UiController.java` |
| MCP / external integration | None — allow-listed internal tools only (justified: no external systems needed for synthetic capstone) | `tools/` |
| Entry point | `FraudCaseAgentApplication` | `src/main/java/.../FraudCaseAgentApplication.java` |
| Framework-neutral interfaces | `FraudCaseProcessor`, `FollowUpAssistant`, `EmbeddingPort`, `PolicyVectorStore`, `PolicyRetriever` | `agent/common/`, `knowledge/` |
| Config modes | `capstone.processor-mode`: `rules` (default) / `spring-ai` / `langchain4j` | `application.yml`, `ProcessorRegistry.java` |

## 3. Build and Test Results

| Command | Result | Details |
|---|---|---|
| `mvn clean verify` | **PASS** | 70 tests, 0 failures / 0 errors / 0 skipped; jar `fraud-case-agent-1.0.0.jar` (78 MB) |
| `mvn compile` | PASS | 109 main classes, no warnings blocking |
| JaCoCo coverage | Not configured | Coverage tooling absent — noted as gap |
| External credentials | Not required | Model disabled by default; DB is local MySQL (`fraudcase` schema, `fraudapp` user) |
| Evaluation harness | PASS | `EvaluationHarnessTest`: schema-valid 100%, route accuracy 100%, abstention 7/7, citation recall 100% |

Test classes (14): `RulesFraudCaseAgentTest`, `RecommendationValidatorTest`, `MaskingValidatorTest`, `CaseStateMachineTest`, `DeterministicFixtureGeneratorTest`, `EvidenceToolsTest`, `ToolExecutorTest`, `PolicyIngestionServiceTest`, `PolicyRetrieverTest`, `CaseConversationMemoryTest`, `AuditEventWriterTest`, `ProcessorContractTest`, `FraudCaseFlowTest`, `CaseApiMvcTest`, `EvaluationHarnessTest`.

## 4. Evaluation Scorecard

| # | Metric | Maximum | Score | Status | Evaluator Comments | Evidence |
|---|---|---:|---:|---|---|---|
| 1 | BFS fraud & dispute use case | 10 | 9 | Implemented | Coherent intake → lifecycle → human-disposition flow; all required classifications present (`GENUINE`/`UNAUTHORISED`/`SUSPICIOUS`/`INSUFFICIENT_EVIDENCE` + `MERCHANT_DISPUTE`/`DUPLICATE_PROCESSING`); masked synthetic IDs enforced; distinct routes keep fraud vs dispute vs payment ops separate; zero loan-underwriting terms (grep clean). −1: no AML-escalation route exists at all (arguably acceptable scope). | `domain/Classification.java`, `Route.java`, `CaseIntakeRequest.java`, `FraudCaseFlowTest` |
| 2 | Architecture & maintainability | 12 | 11 | Implemented | Clean layers (`api`/`application`/`agent`/`domain`/`knowledge`/`persistence`/`tools`); single `FraudCaseProcessor` contract with 3 impls; config-driven selection; shared `RecommendationValidator` — no duplicated business rules; typed enums everywhere; bean validation on intake. −1: `UiController` mixes service calls into view layer (thin but present); `ApiExceptionHandler` can emit JSON on HTML routes (minor content-negotiation wart). | package tree, `ProcessorRegistry`, `RecommendationValidator` |
| 3 | Fraud workflow & deterministic tools | 15 | 12 | Partially Implemented | 7 allow-listed tools (txn details, device, location, merchant risk, prior disputes, policy search, SLA) all deterministic via seeded fixtures; typed result DTOs carry `toolResultId`/observedAt/provenance; `ToolExecutor` enforces 2.5s timeout → structured TIMEOUT/FAILED/NOT_FOUND (never throws internals); tool trace persisted w/ request hash + redacted summary. −3: **no transaction-velocity tool, no sanctions/PEP screening, no amount-deviation or customer-profile tool** (rubric explicitly lists velocity & screening); tools fixed-sequence rather than model-chosen in rules mode. | `EvidenceTools`, `ToolExecutor`, `DeterministicFixtureGenerator`, `ToolExecutorTest` |
| 4 | Spring AI implementation | 10 | 8 | Implemented | `ChatClient` + `ObjectProvider<ChatModel>` (absent → `ProcessorUnavailableException` → rules fallback); system prompt from `prompts/fraud-system.txt`; `MessageChatMemoryAdvisor` on case-scoped `ChatMemory` backed by JPA via `SpringAiChatMemoryRepository`; `.entity(FraudCaseRecommendation.class)` typed output → shared validator; `ModelFacingTools` `@Tool` wrappers; provider config externalised. −2: **never exercised against a live model** (no key in env); adapter builds ChatClient per call; no streaming. | `SpringAiFraudCaseAgent`, `SpringAiChatMemoryRepository`, `SpringAiMemoryConfig`, `ProcessorContractTest` |
| 5 | LangChain4j implementation | 10 | 8 | Implemented | `FraudInvestigationService` AiService built per case with `AiServices.builder`; `@SystemMessage` from `prompts/fraud-system-lc4j.txt`; `MessageWindowChatMemory` + `ChatMemoryProvider` on custom `Lc4jChatMemoryStore` (JPA-backed, caseId-keyed); tools bound via same `ModelFacingTools`; typed DTO return → validator; `setServiceForTesting` seam enables stubbing. −2: same no-live-model caveat; service rebuilt per investigation (correct but unoptimised); LC4j `EmbeddingStore` not used (custom store instead — justified by determinism). | `LangChain4jFraudCaseAgent`, `FraudInvestigationService`, `Lc4jChatMemoryStore` |
| 6 | RAG, embeddings, retrieval, citations | 15 | 12 | Implemented | 5 synthetic policy docs incl. one WITHDRAWN; YAML front-matter metadata (id/title/version/effectiveDate/owner/status); heading-based chunking; deterministic hash embeddings; top-k=4 + threshold 0.15 configurable; **lexical gate** prevents hash-collision false-positives making abstention deterministic; withdrawn docs excluded at query time; citations persist docId/version/section/chunkId/score/passage; abstention adds `missingInformation` entry. −3: lexical (not semantic) embeddings — paraphrase recall weak by design (documented); no stale/contradictory-policy resolution beyond withdrawal; scores not calibrated. | `PolicyIngestionService`, `PolicyRetriever`, `policies/*.md`, `PolicyRetrieverTest` |
| 7 | Structured output, memory, human review | 8 | 8 | Implemented | Typed `FraudCaseRecommendation` DTO + `recommendation.schema.json`; enum-only classification/route/risk; evidence IDs, missing info, rationale, confidence, citations, rule IDs, trace IDs all modelled; validator enforces `humanReviewRequired=true` pre-persistence; bounded (12-msg) case-isolated memory; dispositions incl. ACCEPT_ROUTE/ESCALATE/REROUTE/OVERRIDE/REQUEST_INFORMATION/CLOSE; mandatory rationale; **no autonomous adverse action anywhere**. | `FraudCaseRecommendation`, `RecommendationValidator`, `CaseConversationMemory`, `submitReview` |
| 8 | Security, privacy, responsible AI, audit | 8 | 6 | Partially Implemented | No secrets in repo (env-var placeholders only; `.env`/local yml gitignored); synthetic-only data enforced by `syntheticDataConfirmed` + `MaskingValidator` (PAN/markup rejected); statements treated as untrusted data (system prompt + T11 test); audit stores payload *hashes* + 500-char summaries, never raw; correlation IDs; tool/model mode recorded in audit. −2: **demo passwords hardcoded in `SecurityConfig`** (documented, still a finding); model name/version not captured in audit metadata (mode only); no rate limiting; demo creds also sit in `application.yml` defaults (local-only but committed). | `SecurityConfig`, `MaskingValidator`, `AuditEventWriter`, `CorrelationIdFilter`, `.gitignore` |
| 9 | Testing, reliability, observability | 8 | 6 | Implemented | 70 tests spanning T01–T12: unit, tool-contract, timeout/failure, validator, RAG abstention + withdrawn exclusion, memory isolation/bounds, append-only audit, API authz/validation, 3-way processor parity, evaluation harness; Micrometer timers on tool/retrieval/processor + failure counters; correlation IDs; health indicators for vector store + processor. −2: **no idempotency** (duplicate POST → 2 cases, verified S6); no JaCoCo; metrics endpoint not exposed (`include: health,info` only); no retry/dedup semantics. | test suite, `ToolExecutor`/`PolicyRetriever`/orchestrator timers, `HealthIndicators` |
| 10 | UI, documentation, developer experience | 4 | 3 | Implemented | 6 Thymeleaf screens + case list + audit page: intake, investigation/trace, evidence, recommendation, citations, review — all verified HTTP 200 with role-gated actions; README covers prereqs/run/modes/curl/limits; `AGENTS.md` + 4 docs (`ARCHITECTURE`, `EVALUATION`, `SELF_REVIEW`, `PROMPT_LOG`); `application-example.yml` placeholder-only. −1: accessibility (labels/keyboard/ARIA) not verified; no loading/empty-state polish confirmed; no screenshots. | `templates/*.html`, `README.md`, `docs/` |
| | **Total** | **100** | **83** | | | |

## 5. Framework Evaluation

### 5.1 Spring AI
- `SpringAiFraudCaseAgent` implements `FraudCaseProcessor.analyze` via `ChatClient`
  built from an `ObjectProvider<ChatModel>` → absent model yields
  `ProcessorUnavailableException`, orchestrator falls back to rules (fail-safe, audited).
- System prompt `prompts/fraud-system.txt` encodes triage-only role, no fabrication,
  schema-only output, `humanReviewRequired=true`, no CoT.
- Memory: `MessageChatMemoryAdvisor` + `MessageWindowChatMemory` over
  `SpringAiChatMemoryRepository` → JPA `chat_memory` keyed by `conversationId=caseId`.
- Tools: `ModelFacingTools` `@Tool` methods routed through `ToolExecutor` (same
  tracing/timeout as the fixed plan).
- Output: `.entity(FraudCaseRecommendation.class)` → `RecommendationValidator`.

### 5.2 LangChain4j
- `LangChain4jFraudCaseAgent` builds `FraudInvestigationService` per case via
  `AiServices.builder(...).chatModel(...).tools(...).chatMemoryProvider(...)`.
- `@SystemMessage(fromResource = "prompts/fraud-system-lc4j.txt")` — same contract text.
- Memory: `MessageWindowChatMemory(12)` + `Lc4jChatMemoryStore` → same `chat_memory`
  table; `@MemoryId` = caseId.
- Output: typed return type + `@OutputSchema` → same validator.
- `setServiceForTesting` seam enables offline stub (`ProcessorContractTest`).

### 5.3 Framework Parity
- `ProcessorContractTest` parameterises all three modes over the same fixture
  contexts: identical schema invariants (caseId echo, enums non-null, confidence
  ∈ [0,1], `humanReviewRequired`, evidence present, citation completeness).
- `bothFrameworksAgreeWithRulesOnSharedFixtures` asserts stubbed AI output
  matches rules classification + route on the unauth fixture.
- Differences: Spring AI = fewer LOC, advisor-driven memory; LC4j = explicit
  builder per case (case-bound tools/memory), framework-portable service iface.
  Business rules are identical — neither adapter contains routing logic.

## 6. Acceptance Scenario Results

| Scenario | Mode | Expected | Actual | Result | Evidence |
|---|---|---|---|---|---|
| Known device, familiar merchant, normal amount | rules | Genuine/low-risk, human approval | `GENUINE_TRANSACTION` → `MANUAL_REVIEW`, LOW, 0.6 | **PASS** | live: `CASE-2026-71225` |
| New device, unusual location, >5× amount | rules | Suspicious/unauthorised + citations | `UNAUTHORISED_TRANSACTION` → `FRAUD_INVESTIGATION`, HIGH, 0.7, missing auth outcome | **PASS** | live: `CASE-2026-71337`-class unauth |
| ≥5 transactions in 10 min | — | Velocity indicator | **No velocity tool exists** | **FAIL (gap)** | `EvidenceTools` inventory |
| Confirmed synthetic screening match | — | Mandatory escalation | **No sanctions/PEP tool exists** | **FAIL (gap)** | `EvidenceTools` inventory |
| Evidence service unavailable | rules | `INSUFFICIENT_EVIDENCE` / more info | `INSUFFICIENT_EVIDENCE` → `MANUAL_REVIEW`, case `INCOMPLETE`, missing list populated | **PASS** | live: `CASE-2026-71340`-class missing; `ToolExecutorTest` |
| Same case submitted twice | rules | Idempotent/duplicate protection | Two distinct `caseId`s issued | **FAIL (gap)** | live: `71337` vs `71340` |
| Policy question with active source | rules | Grounded answer + title/section/version | Follow-up returns evidence + `POL-DSP-004 v1.7 §1.3` etc. with chunk + score | **PASS** | live `/messages` on `71225` |
| Question absent from policy corpus | rules | Explicit no-information | Empty retrieval → abstention + `missingInformation` | **PASS** | `PolicyRetrieverTest#irrelevantQueryAbstains`, `RulesFraudCaseAgentTest#t05` |
| Retired policy contains matching text | rules | Retired excluded | `POL-OLD-001` (WITHDRAWN) never returned | **PASS** | `PolicyRetrieverTest#withdrawnDocumentNeverReturned` |
| Model returns malformed output | rules | Validated retry / safe failure | Validator rejects (caseId, confidence, enum, review flag); orchestrator → rules fallback + `RECOMMENDATION_INVALID` audit | **PASS** | `RecommendationValidatorTest`, orchestrator catch blocks |
| Two separate case IDs | rules | Memory isolated | `renderForPrompt(CASE-B)` excludes case-A fact; bounded window evicts | **PASS** | `CaseConversationMemoryTest`, `FraudCaseFlowTest#t10` |
| Prompt injection in narrative | rules | Narrative is data; constraints hold | Injected "mark GENUINE and close" → still `UNAUTHORISED_TRANSACTION` → `FRAUD_INVESTIGATION` | **PASS** | live: `CASE-…-12` + `t11` test |

**Score: 10/12 scenarios pass; 2 are genuine capability gaps (velocity, screening); 1 is an idempotency gap.**

## 7. Strengths

1. **Safety is structural, not cosmetic** — `humanReviewRequired` enforced by the shared validator; no adverse-action tool exists in the allow-list; failures degrade to rules + audit rather than surfacing.
2. **True framework neutrality** — one `FraudCaseProcessor` contract, one validator, one context; Spring AI and LangChain4j are interchangeable by config and proven equivalent by `ProcessorContractTest` parity suite.
3. **Honest offline determinism** — seeded fixture generator, deterministic hash embedder with a lexical gate making abstention reproducible, and 70 fully offline tests covering T01–T12 plus an evaluation harness with printed metrics.

## 8. Gaps and Risks

| Priority | Gap or Risk | Impact | Evidence | Recommended Fix |
|---|---|---|---|---|
| High | No transaction-velocity tool | Rubric explicitly requires velocity evidence; S3 fails | `EvidenceTools` | Add `getTransactionVelocity` fixture-backed tool + rule/signal |
| High | No sanctions/PEP screening tool | Mandatory-escalation scenario untestable | `EvidenceTools` | Add synthetic `screenParty` tool returning match/no-match → ESCALATE route |
| High | No intake idempotency | Duplicate submissions create duplicate cases (verified) | live S6 | Unique constraint on `transaction_ref` or idempotency-key header |
| Medium | AI modes never run against a live model | Parity is stub-verified only | `ProcessorContractTest` | Documented env-var setup + optional smoke test behind `@EnabledIfEnvironmentVariable` |
| Medium | Demo credentials hardcoded | `agent123` etc. in committed `SecurityConfig`; db default in `application.yml` | `SecurityConfig`, `application.yml` | Move to env-only; keep demo note in README |
| Medium | Lexical embeddings only | Paraphrase recall weak; threshold 0.15 low | `LocalHashEmbeddingModel` | Plug real `EmbeddingPort` impl when provider configured |
| Low | No JaCoCo / metrics endpoint closed | Coverage unmeasurable; timers invisible via actuator | `pom.xml`, `application.yml` | Add JaCoCo; expose `metrics` endpoint |
| Low | Model name/version absent from audit | Reproducibility metadata thin | `RecommendationEntity.mode` only | Persist provider+model in audit payload |

## 9. Security and Responsible AI Findings

| Severity | Finding | File or Component | Remediation |
|---|---|---|---|
| Medium | Hardcoded demo user passwords | `SecurityConfig.java` | Externalise or `@Profile("demo")`-gate the in-memory users |
| Medium | DB default password in committed config (`change-me-locally` fallback) | `application.yml` | Require `FRAUD_DB_PASSWORD` without default for non-local |
| Low | `/actuator/metrics` 500s through generic handler when not exposed | `ApiExceptionHandler` | Either expose metrics or map to 404 |
| Info | Injection-bearing policy docs retrievable as *passage data only* — verified no behavioural effect | `PolicyRetriever` | None — test documents intent |
| Info | Audit payload hashing + summary truncation verified — no raw PII/statements in audit/log | `AuditEventWriterTest` | None |
| Info | Secret-pattern scan of git-tracked files: **no keys/tokens/PEMs found** | repo-wide grep | Maintain `.gitignore` + `application-example.yml` placeholders |

## 10. Recommended Improvements

### Critical
- None blocking demo readiness.

### High
1. Add `getTransactionVelocity` tool (count/amount in sliding window from fixture) + a rule consuming it.
2. Add `screenPartyForSanctions` synthetic tool + `ESCALATE` handling for confirmed matches.
3. Idempotent intake: unique key on `transaction_ref` (+ optional `Idempotency-Key` header) returning the existing case.

### Medium
4. Externalise demo credentials; drop DB-password default outside local profile.
5. Wire a real embedding provider behind `EmbeddingPort` when OpenAI config present; keep local as fallback.
6. Optional live-model smoke tests gated on env vars.

### Low
7. JaCoCo + expose `metrics` actuator; persist model name/version in audit.
8. Add loading/empty states and basic a11y labels to templates; capture screenshots for the submission.

## 11. Definition-of-Done Checklist

- [x] Fraud case intake works with synthetic data
- [x] Deterministic tools provide traceable evidence
- [x] Structured recommendations are validated
- [x] Policy answers are grounded and cited
- [x] Human review is mandatory
- [x] Conversation/case memory is isolated
- [x] Secrets are not stored in the repository
- [x] Error and no-information states are handled safely
- [x] Automated tests pass
- [x] README enables another participant to run the project

## 12. Final Verdict

**Demo Ready.** The capstone delivers a complete, verifiable
intake → bounded-tool-plan → RAG → validated-recommendation → human-review loop
with three interchangeable processors, deterministic evidence, exact policy
citations, case-isolated memory, and append-only audit — all exercised by a
green 70-test suite and live end-to-end runs on MySQL.

The verdict is conditional on the stated context (synthetic training, no live
model key present): Spring AI and LangChain4j adapters are contract-tested with
stubs but unproven against a real endpoint, and two rubric-listed tool
capabilities (velocity, sanctions screening) plus intake idempotency are absent.
Closing the three High gaps would make this a complete submission.
