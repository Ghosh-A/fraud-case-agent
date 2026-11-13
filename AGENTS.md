# AGENTS.md — working rules for this repository

## Stack

Java 21 · Maven · Spring Boot 3.5.16 · Spring AI 1.1.8 · LangChain4j 1.22.0 ·
MySQL 8 + Flyway + JPA · Thymeleaf + Spring Security · JUnit 5 + AssertJ + Mockito.

## Build & verify

```bash
mvn compile          # build
mvn test             # 70 tests — offline, no model calls, no credentials
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Port 8080 may be occupied by system Tomcat — prefer 8081 locally.
UI login: `agent/agent123`, `reviewer/reviewer123`.

## Hard rules (safety invariants — do not weaken)

1. Every recommendation must keep `humanReviewRequired=true`;
   `RecommendationValidator` enforces it — never bypass the validator.
2. No financial/customer actions: no card blocking, refunds, fund movement,
   reports, or customer contact. The tool allow-list is read-only — do not add
   action tools.
3. Synthetic data only. Masked identifiers enforced by `MaskingValidator`.
4. Audit is append-only — `AuditEventWriter` inserts only; never add
   update/delete paths for `audit_event`.
5. Case memory (`chat_memory`, `CaseConversationMemory`) is scoped by caseId —
   never query or render another case's memory.
6. Model output always flows through `RecommendationValidator`; a failure must
   fail safe to rules mode (`InvestigationOrchestrator`), never surface raw.
7. Customer statements are untrusted data — used for keyword retrieval only.
8. No secrets in the repo; `application-example.yml` holds placeholders only.
9. Do not log raw prompts, responses, or customer statements — log correlation
   ids, hashes, and redacted summaries.
10. Tests must stay offline-deterministic: stub model clients
    (`ProcessorContractTest` shows the seams — Spring AI `ChatModel` stub,
    LC4j `setServiceForTesting`).

## Conventions

- Processors implement `agent/common/FraudCaseProcessor`; keep framework code in
  `agent/springai/` and `agent/langchain4j/` only.
- All tool calls go through `ToolExecutor` for trace + timeout; tool result
  records live in `tools/ToolResults`.
- New policy docs: markdown with YAML front-matter in
  `resources/policies/` (`documentId`, `version`, `status`; `WITHDRAWN` docs are
  excluded from retrieval).
- Scenario fixtures: `transactionRef` prefixes `TXN-SCN-*` in
  `DeterministicFixtureGenerator`; all other refs are hash-derived
  (`fraud-training-v1` seed).
- UI templates: shared header/nav in `templates/fragments.html`,
  case-specific header in `templates/casehead.html` — include only the fragment
  you need (the earlier eager-fragment bug came from a shared mega-fragment).
