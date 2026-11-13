# Evaluation Report

Harness: `src/test/java/com/capstone/fraudcase/EvaluationHarnessTest.java` —
runs the labelled fixture set through the rules processor and asserts metrics
as a regression gate. Latest run output:

```
=== evaluation summary ===
  unauth         UNAUTHORISED_TRANSACTION -> FRAUD_INVESTIGATION
  dispute        MERCHANT_DISPUTE -> DISPUTE_OPERATIONS
  duplicate      DUPLICATE_PROCESSING -> PAYMENT_OPERATIONS
  missing-txn    INSUFFICIENT_EVIDENCE -> MANUAL_REVIEW
  no-policy      UNAUTHORISED_TRANSACTION -> MANUAL_REVIEW
  contradictory  SUSPICIOUS_TRANSACTION -> MANUAL_REVIEW
  genuine        GENUINE_TRANSACTION -> MANUAL_REVIEW
  schema-valid: 100%
  route accuracy: 100%
  abstention correctness: 7/7
  citation recall: 100%
```

## Measures

| Metric | Definition | Result |
|---|---|---|
| Schema-valid % | recommendations passing `RecommendationValidator` | 100% (7/7) |
| Route accuracy | recommended route == labelled expected route | 100% (7/7) |
| Abstention correctness | abstain (`MANUAL_REVIEW`/`INSUFFICIENT_EVIDENCE`) exactly when expected | 7/7 |
| Citation recall | non-abstaining cases carry ≥1 policy citation | 100% |
| Determinism | identical context → identical output | asserted (`deterministicSameInputSameOutput`) |

## Scenario coverage (T01–T12)

| Req | Fixture | Mechanism verified |
|---|---|---|
| T01 | `TXN-SCN-UNAUTH-*` | new device + HIGH location anomaly → `FRAUD_INVESTIGATION` |
| T02 | `TXN-SCN-DISPUTE-*` | recognised merchant + cancellation → `DISPUTE_OPERATIONS` |
| T03 | `TXN-SCN-DUP-*` | duplicate fingerprint → `PAYMENT_OPERATIONS` |
| T04 | `TXN-SCN-MISSING-*` | txn not found → `INCOMPLETE` + `INSUFFICIENT_EVIDENCE` |
| T05 | no-policy context | 0 citations → `MANUAL_REVIEW` + missing-info |
| T06 | contradictory context | trusted device + HIGH anomaly → lowered confidence, manual |
| T07 | spring-ai without model | `ProcessorUnavailableException`; rules-mode flow unaffected |
| T08 | sleeping tool | `TIMEOUT` status + trace, structured unavailable result |
| T09 | bad confidence / enum / caseId | validator + JSON parse rejections |
| T10 | two cases | memory isolation + bounded window |
| T11 | injected statement text | treated as data; pipeline unchanged |
| T12 | blank review reason | `IllegalArgumentException`; role 403 for non-reviewer |

## Limitations of this evaluation

- Fixtures are deterministic and small (7 labelled cases) — high scores are
  expected by construction; the value is regression protection, not accuracy
  claims against real fraud data.
- AI-adapter parity is asserted on stubbed model output, not live models.
- Citation quality is measured by presence, not human-judged relevance.
- No latency benchmark beyond recorded tool/processor timers.
