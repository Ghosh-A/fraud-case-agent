# Architecture

## 1. Design goal

An **explainable** agentic assistant: every recommendation must be traceable to
tool-produced evidence and exact policy citations, and a human always makes the
final call. "Agentic" means the processor can invoke allow-listed tools and
consult retrieved policy — it does not mean it can act on the world.

## 2. Layers

```
┌─────────────────────────── adapters ───────────────────────────┐
│  CaseApiController (REST)   UiController (Thymeleaf)            │
│  SecurityConfig · CorrelationIdFilter · ApiExceptionHandler     │
└────────────────────────────────────────────────────────────────┘
┌──────────────────────── application ────────────────────────────┐
│  FraudCaseService        – intake, review, follow-up, queries    │
│  InvestigationOrchestrator – evidence + retrieval + processor    │
│  FraudCaseProcessorRegistry – mode selection (rules default)     │
│  CaseConversationMemory   – per-case bounded chat history        │
└────────────────────────────────────────────────────────────────┘
┌───────────────────────── processors ────────────────────────────┐
│  FraudCaseProcessor (framework-neutral contract)                 │
│    RulesFraudCaseAgent        R-00…R-05 on CollectedSignals      │
│    SpringAiFraudCaseAgent     ChatClient + tools + advisor       │
│    LangChain4jFraudCaseAgent  AiServices + tools + memory        │
│  RecommendationValidator – shared schema/business rules          │
└────────────────────────────────────────────────────────────────┘
┌──────────── tools ────────────────┐  ┌────────── knowledge ─────┐
│ EvidenceTools ×7 (allow-listed)   │  │ PolicyIngestionService   │
│ DeterministicFixtureGenerator     │  │ LocalHashEmbeddingModel  │
│ ToolExecutor (trace + timeout)    │  │ InMemoryPolicyVectorStore│
│ ModelFacingTools (@Tool wrappers) │  │ PolicyRetriever          │
└───────────────────────────────────┘  └──────────────────────────┘
┌────────────────────── persistence (H2 + Flyway) ─────────────────┐
│ fraud_case · transaction_claim · evidence_item · recommendation  │
│ policy_citation · tool_trace · review_disposition · audit_event  │
│ chat_memory                                                      │
└──────────────────────────────────────────────────────────────────┘
```

## 3. Investigation data flow

1. `createCase` validates intake (masking, enums, synthetic-data attestation) → `NEW`.
2. `investigate` → `ENRICHING`; for each of 7 evidence calls `ToolExecutor`
   runs it under a timeout and records a `tool_trace` (name, request hash,
   status, duration, redacted summary). Failures become structured
   `NOT_FOUND`/`TIMEOUT`/`FAILED` results — never fabricated facts.
3. `PolicyRetriever` builds a query from issue type, channel, recognition flag,
   signal flags and ≤12 statement tokens; hits must pass the cosine threshold
   **and** share ≥1 real token with the query (guards hash-collision noise and
   makes abstention deterministic). Withdrawn documents are excluded at
   ingestion.
4. Signals + evidence + citations assemble `FraudCaseContext`.
5. The configured `FraudCaseProcessor` analyses the context. AI processors may
   call `ModelFacingTools` for the same allow-listed operations; output parses
   into `FraudCaseRecommendation` then passes `RecommendationValidator`
   (caseId match, confidence bounds, `humanReviewRequired=true`, citation
   completeness, evidence presence unless `INSUFFICIENT_EVIDENCE`).
6. Validation/processor failure → `OUTPUT_INVALID` audit + safe fallback to
   rules; still ends at `READY_FOR_REVIEW` or `INCOMPLETE`.
7. `READY_FOR_REVIEW` → a human submits `ACCEPT_ROUTE`/`REROUTE`/`OVERRIDE`/
   `ESCALATE`/`REQUEST_INFORMATION`/`CLOSE` **with mandatory rationale** →
   `ROUTED`/`CLOSED`/`INCOMPLETE`.

## 4. Case states

```
NEW → ENRICHING → READY_FOR_REVIEW → UNDER_REVIEW → ROUTED → CLOSED
          ↑↓              │
      INCOMPLETE ←────────┘  (REQUEST_INFORMATION / missing evidence)
```

`CaseStateMachine` rejects every transition outside this graph (e.g. no
`READY_FOR_REVIEW → ROUTED` without `UNDER_REVIEW`).

## 5. Explainability surfaces

- **Evidence screen** — items with provenance (tool result id, origin, observed
  time, status, summary).
- **Recommendation screen** — classification, risk, route, confidence, rule ids
  or model mode, evidence ids, missing information.
- **Citations screen** — documentId, version, section, chunkId, score, passage.
- **Audit screen** — append-only event log per case; `X-Correlation-Id` ties
  every request to application/tool/model logs.
- No hidden chain-of-thought is requested or displayed; prompts ask for a
  concise evidence-linked rationale only.

## 6. Framework parity (Spring AI vs LangChain4j)

| Aspect | Spring AI | LangChain4j |
|---|---|---|
| Agent | `SpringAiFraudCaseAgent` | `LangChain4jFraudCaseAgent` |
| Service | `ChatClient` | `AiServices` interface `FraudInvestigationService` |
| Memory | `MessageWindowChatMemory` + `SpringAiChatMemoryRepository` over `chat_memory` | `MessageWindowChatMemory` + `Lc4jChatMemoryStore` over `chat_memory` |
| Tools | `.tools(modelFacingTools)` `@Tool` methods | `.tools(...)` on the same `ModelFacingTools` |
| Output | `.entity(FraudCaseRecommendation.class)` | method return type + `@OutputSchema` |
| Follow-up | `SpringAiFollowUpAssistant` | `LangChain4jFollowUpAssistant` |
| Shared | identical `FraudCaseContext`, prompts, `RecommendationValidator`, citations, fallback | same |

`ProcessorContractTest` runs identical contexts through all three modes with
stubbed models and asserts the same schema invariants; the parity test verifies
stubbed AI output agrees with rules on shared fixtures.

## 7. Prompt / tool contract

- System prompts (`resources/prompts/`) fix the role: a triage assistant, never
  a decision-maker; must use only supplied facts/tools/policy; must list missing
  or contradictory info; must return only the recommendation schema;
  `humanReviewRequired` always true.
- `recommendation.schema.json` is the agreed output schema, referenced by both
  frameworks' output conversion.
- Tool exposure to the model is the `@Tool`-annotated `ModelFacingTools` facade
  — a strict subset of `EvidenceTools`, still routed through `ToolExecutor`.

## 8. Security & privacy

- Spring Security: form login for UI, HTTP Basic accepted for `/api/**`;
  `/api/**` is CSRF-exempt (machine clients), UI forms keep CSRF tokens.
- Roles: `AGENT` (intake/investigate/follow-up), `REVIEWER` (dispositions),
  `AUDITOR` (audit read). Demo users are in-memory — training only.
- Intake enforces masked references and rejects PAN-like digits and markup.
- No secrets in repo; `application-example.yml` carries placeholders only.
- Logs carry correlation ids and redacted summaries — no raw prompts, no
  customer statements in logs.
