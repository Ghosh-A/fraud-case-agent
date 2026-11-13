# Fraud Case Agent — Explainable Agentic Triage Assistant

A Spring Boot application that investigates synthetic payment-fraud dispute cases
through an agentic pipeline: deterministic evidence tools, thresholded policy
retrieval (RAG), a structured routing recommendation — and a **mandatory human
review** before any disposition.

The agent only **recommends**. It never blocks a card, issues a refund, moves
funds, files a report, or contacts a customer. Every recommendation cites
evidence and policy passages, and every decision ends with a human reviewer.

## Stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 21, Maven |
| Framework | Spring Boot 3.5.16 |
| AI frameworks | Spring AI 1.1.8 **and** LangChain4j 1.22.0 (behind one contract, parity-tested) |
| Persistence | MySQL 8 + Flyway + Spring Data JPA |
| UI | Thymeleaf + Spring Security (session login, CSRF on forms) |
| Embeddings | `LocalHashEmbeddingModel` — deterministic, offline, reproducible |
| Model provider | Any OpenAI-compatible endpoint (config-driven, optional) |

## Architecture

```
intake (REST/Thymeleaf)
   │
   ▼
FraudCaseService ── audit ──────────────────────────────┐
   │ createCase → NEW                                   │
   ▼                                                    │
InvestigationOrchestrator            (case-scoped memory)
   │ ENRICHING                                          │
   ├─ EvidenceTools ×7   via ToolExecutor (timeout → structured result)
   ├─ PolicyRetriever    via InMemoryPolicyVectorStore (threshold + lexical gate)
   │
   ▼  FraudCaseContext (summary + signals + evidence + citations)
FraudCaseProcessor   ← one interface, three implementations
   ├─ RulesFraudCaseAgent         (deterministic, always available)
   ├─ SpringAiFraudCaseAgent      (ChatClient + tools + case memory)
   └─ LangChain4jFraudCaseAgent   (AiServices + tools + case memory)
   │
   ▼  FraudCaseRecommendation (validated against shared schema)
READY_FOR_REVIEW ── human submits disposition ──► ROUTED / CLOSED / INCOMPLETE
```

See `docs/ARCHITECTURE.md` for the full data flow and design decisions.

## Quick start (no API key needed)

Prerequisite — a local MySQL 8 instance with the app database and user:

```sql
CREATE DATABASE fraudcase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fraudapp'@'localhost' IDENTIFIED BY 'change-me-locally';
GRANT ALL PRIVILEGES ON fraudcase.* TO 'fraudapp'@'localhost';
```

Override connection settings via env vars if needed: `FRAUD_DB_URL`,
`FRAUD_DB_USER`, `FRAUD_DB_PASSWORD` (defaults shown above — local dev only).

```bash
mvn spring-boot:run            # defaults to rules mode on port 8080
# or a free port:
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

UI: `http://localhost:8081/` → log in as `agent` / `agent123`
(fraud-analyst role) or `reviewer` / `reviewer123` (reviewer role).

### REST flow

```bash
# 1. create a case (HTTP Basic; /api/** is CSRF-exempt, browser forms are not)
curl -u agent:agent123 -X POST localhost:8081/api/cases \
  -H 'Content-Type: application/json' -d '{
    "maskedCustomerRef":"CUST-••••-4821","homeRegion":"IN-KA",
    "transactionRef":"TXN-SCN-UNAUTH-001","amount":48750.00,"currency":"INR",
    "occurredAt":"2026-11-06T08:42:00+05:30","status":"POSTED",
    "channel":"CARD_NOT_PRESENT","maskedMerchant":"NORTHSTAR DIGITAL ••••73",
    "issueType":"MERCHANT_NOT_RECOGNISED",
    "customerStatement":"I do not recognise this purchase and still have my card.",
    "cardPossession":"HAS_CARD","merchantRecognised":false,
    "intakeChannel":"WEB","reportedAt":"2026-11-13T10:00:00Z",
    "syntheticDataConfirmed":true,"reviewRequired":true}'

# 2. run the investigation
curl -u agent:agent123 -X POST localhost:8081/api/cases/CASE-XXX/investigate

# 3. inspect evidence / traces / recommendation / citations / follow-up
curl -u agent:agent123 localhost:8081/api/cases/CASE-XXX/evidence
curl -u agent:agent123 localhost:8081/api/cases/CASE-XXX/tool-traces
curl -u agent:agent123 localhost:8081/api/cases/CASE-XXX/recommendation
curl -u agent:agent123 localhost:8081/api/cases/CASE-XXX/citations
curl -u agent:agent123 -X POST localhost:8081/api/cases/CASE-XXX/messages \
  -H 'Content-Type: application/json' -d '{"question":"why did you route this to fraud?"}'

# 4. human review (reviewer role required; rationale mandatory)
curl -u reviewer:reviewer123 -X POST localhost:8081/api/cases/CASE-XXX/reviews \
  -H 'Content-Type: application/json' \
  -d '{"disposition":"ACCEPT_ROUTE","reason":"signals and policy support fraud routing"}'
```

## Processor modes

Set `capstone.processor-mode` (env `CAPSTONE_PROCESSOR_MODE`):

| Mode | Behaviour |
|---|---|
| `rules` *(default)* | Deterministic rules R-00…R-05 over collected signals. No model needed. |
| `spring-ai` | `ChatClient` with allow-listed `@Tool`s, `MessageChatMemoryAdvisor`, `.entity()` structured output. |
| `langchain4j` | `AiServices` interface with tools + `ChatMemoryProvider` + `@OutputSchema` output contract. |

Both AI modes use the **same** `FraudCaseContext`, prompt contract, tool allow-list,
`RecommendationValidator`, and citation records — only the framework integration
differs (see `docs/ARCHITECTURE.md` §6 and `ProcessorContractTest`).

### Connecting a real model (optional)

```bash
export OPENAI_API_KEY=sk-...
export OPENAI_BASE_URL=https://api.openai.com   # or any compatible endpoint
export OPENAI_CHAT_MODEL=gpt-4o-mini
export CAPSTONE_PROCESSOR_MODE=spring-ai        # or langchain4j
mvn spring-boot:run
```

`application-example.yml` shows every variable with placeholders only — **no
secrets are committed**. If the model is unavailable or returns malformed JSON,
the orchestrator falls back to `rules` and records an `OUTPUT_INVALID` /
`TOOL_FAILED` audit event (fail-safe, never silent).

## Synthetic scenario references

`transactionRef` prefixes select labelled fixtures (`fraud-training-v1` seed),
everything else is hash-derived:

| Prefix | Fixture |
|---|---|
| `TXN-SCN-UNAUTH-*` | Unrecognised merchant, new device, high location anomaly |
| `TXN-SCN-DISPUTE-*` | Recognised merchant, cancellation/dispute complaint |
| `TXN-SCN-DUP-*` | Duplicate payment fingerprint |
| `TXN-SCN-MISSING-*` | Transaction not found → `INCOMPLETE` |
| anything else | Deterministic hash-based signals |

## Tests

```bash
mvn test          # 70 tests — no model calls, no credentials
```

Coverage by requirement:

| Req | Covered by |
|---|---|
| T01/T02/T03 routing | `RulesFraudCaseAgentTest`, `FraudCaseFlowTest` |
| T04 missing evidence | `RulesFraudCaseAgentTest#t04`, `FraudCaseFlowTest#t04` |
| T05 policy abstention | `RulesFraudCaseAgentTest#t05`, `PolicyRetrieverTest` |
| T06 contradictions | `RulesFraudCaseAgentTest#t06` |
| T07 model unavailable | `ProcessorContractTest#springAiWithoutModelFailsSafely`, rules-mode `FraudCaseFlowTest` |
| T08 tool timeout | `ToolExecutorTest` |
| T09 malformed output | `RecommendationValidatorTest` (incl. enum rejection) |
| T10 memory isolation | `CaseConversationMemoryTest`, `FraudCaseFlowTest#t10` |
| T11 injection | `FraudCaseFlowTest#t11` |
| T12 review rationale | `FraudCaseFlowTest#t12`, `CaseApiMvcTest` |
| Framework parity | `ProcessorContractTest` (all 3 processors, same schema invariants) |
| API contract/authz | `CaseApiMvcTest` |

## Safety boundaries (enforced, not documented-only)

- `humanReviewRequired` is validated `true` on every recommendation —
  the validator throws otherwise.
- Tools are read-only and allow-listed; there is no financial-action tool.
- Customer statements are data only; they are tokenised for retrieval but never
  executed (prompt-injection attempts have no effect in rules mode — see T11).
- Masked identifiers enforced at intake (`MaskingValidator` rejects PANs/markup).
- Audit events are append-only (no update/delete endpoints; writer service only
  inserts).
- Raw prompts/responses are not logged; tool traces store request hashes and
  redacted summaries.

## Repository layout

```
src/main/java/com/capstone/fraudcase/
  api/            REST + Thymeleaf controllers, security, error handler, correlation filter
  agent/          common contract + rules/, springai/, langchain4j/ implementations
  application/    service, orchestrator, case memory, processor registry
  domain/         enums, state machine, masking validation
  knowledge/      embedding port, vector store, ingestion, retriever
  persistence/    entities, repositories, audit writer
  tools/          deterministic fixtures, evidence tools, traced executor, model-facing tools
src/main/resources/
  db/migration/   Flyway schema
  policies/       synthetic policy corpus (ACTIVE + one WITHDRAWN doc)
  prompts/        system/user prompt templates per framework
  templates/      6 UI screens + shared fragments
docs/             architecture, self-review
```

## Known limitations

- Embeddings are lexical (hashed unigrams/bigrams), not semantic — deliberately
  deterministic for training; swap `EmbeddingPort` for a real model later.
- `InMemoryPolicyVectorStore` resets on restart (re-ingested at boot; case data
  persists in MySQL).
- Demo users are in-memory (`agent`/`reviewer`) — replace for any real use.
- Tests run against the same `fraudcase` schema — they leave their rows
  behind, so wipe it if you want a clean demo.
- See `docs/SELF_REVIEW.md` for residual risks and the Spring AI vs LangChain4j
  comparison.
