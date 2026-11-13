# Self-Review & Evaluation

## What was built

All seven phases of the plan are implemented and verified:

1. **Foundation** — Maven skeleton, pinned versions, config-driven properties,
   domain enums, masking validation, state machine.
2. **Pipeline** — Flyway/JPA persistence (9 entities), append-only audit writer,
   7 deterministic evidence tools behind a traced `ToolExecutor`,
   `RulesFraudCaseAgent` (R-00…R-05), shared `RecommendationValidator`.
3. **AI adapters** — Spring AI and LangChain4j agents + follow-up assistants,
   framework-specific prompts, case-scoped memory bridges into `chat_memory`,
   mode registry with rules fallback.
4. **RAG** — policy ingestion with YAML-front-matter metadata, deterministic
   hash embeddings, thresholded retrieval with a lexical gate, exact citations
   (doc/version/section/chunk/score/passage), abstention on no match.
5. **API + UI** — REST endpoints, role-based security, CSRF handling,
   correlation ids, 6 Thymeleaf screens (list, intake, investigation/evidence,
   recommendation, citations, review, audit), follow-up endpoint.
6. **Observability + tests** — 70 tests covering T01–T12 plus unit, contract,
   evaluation and parity layers; correlation ids; tool traces; Micrometer
   timers on tool/retrieval/processor paths; health indicators.
7. **Docs** — README, this file, architecture doc, evaluation report, prompt
   log, `AGENTS.md`, example config.

## Evaluation

### Scenario matrix (T01–T06) — automated in `FraudCaseFlowTest` + `RulesFraudCaseAgentTest`

| Case | Expected | Observed (tests green) |
|---|---|---|
| Unauthorised CNP, new device, high location anomaly | `UNAUTHORISED_TRANSACTION` → `FRAUD_INVESTIGATION`, high risk | ✓ (R-03) |
| Recognised merchant, cancellation complaint | `MERCHANT_DISPUTE` → `DISPUTE_OPERATIONS` | ✓ (R-04) |
| Duplicate fingerprint | `DUPLICATE_PROCESSING` → `PAYMENT_OPERATIONS` | ✓ (R-02) |
| Transaction not found | `INCOMPLETE` + `INSUFFICIENT_EVIDENCE` | ✓ (R-01) |
| No policy match | `MANUAL_REVIEW` + missing-info "policy guidance unavailable" | ✓ (R-05) |
| Contradictory signals | `SUSPICIOUS_TRANSACTION` → `MANUAL_REVIEW`, confidence < 0.6 | ✓ |

### Safety properties — automated

- `humanReviewRequired` enforced by validator (rejection test included).
- Review rationale mandatory (`t12`); role checks (`CaseApiMvcTest`).
- Memory isolation & bounded window (`CaseConversationMemoryTest`).
- Timeout → structured `TIMEOUT` result + trace (`ToolExecutorTest`).
- Injection text treated as data (`t11`); masking rejects PANs (`MaskingValidatorTest`).
- Withdrawn policy never retrievable; junk queries abstain (`PolicyRetrieverTest`).

### Manual smoke verification (running app, rules mode)

- Full REST journey: create → investigate → evidence → recommendation →
  citations → review → follow-up. ✓
- All six UI screens render 200 after login (initial fragment bug fixed by
  splitting `casehead.html`). ✓

## Spring AI vs LangChain4j — practical comparison

| | Spring AI | LangChain4j |
|---|---|---|
| Integration feel | Spring-native (`ChatClient` bean, advisors, `ObjectProvider`) | Standalone builder API; framework-agnostic service interfaces |
| Structured output | `.entity(Class)` — output converter + validation hook | `@OutputSchema` + return-type conversion |
| Memory | `ChatMemory` + `ChatMemoryRepository` adapter | `ChatMemoryProvider` + `ChatMemoryStore` adapter |
| Tool binding | `.tools(bean)` with `@Tool` | `.tools(bean)` with `@Tool` — nearly identical |
| Boilerplate | Advisor chain config | Explicit `AiServices.builder(...)` per case (for case-bound tools) |
| Verdict | Slightly less code if already Spring-heavy | Slightly more explicit control; easier to port outside Spring |

Both produced equivalent recommendations on stubbed fixtures; parity is enforced
by `ProcessorContractTest`. The domain contract `FraudCaseProcessor` kept the
swap transparent — service code never references either framework.

## Residual risks & limitations

- **Lexical embeddings** — `LocalHashEmbeddingModel` is intentionally
  deterministic; real semantic retrieval needs a proper embedding model behind
  `EmbeddingPort`. The lexical gate compensates for hash noise but cannot match
  paraphrases.
- **In-memory vector store** — re-ingested each boot; fine for 5 docs, not for a
  real corpus.
- **Fixture realism** — deterministic tools cover the labelled scenarios well;
  arbitrary refs get hash-derived signals that may not be coherent across tools.
- **Demo auth** — in-memory users, no password policy; not for real deployment.
- **No screenshots** — UI verified by HTTP status + rendered markup; capture
  screenshots during the live demo.
- **LC4j service rebuild per case** — tools are bound per investigation; a live
  deployment could cache the `ChatModel` and only rebuild the service (current
  approach is correct, just not optimised).
- **Single-turn follow-up** — memory persists across follow-ups, but there's no
  streaming; fine for training.

## Deliberate non-features (safety)

No card blocking, refunds, fund movement, report filing, or customer contact —
by design; no such tool exists in the allow-list.
