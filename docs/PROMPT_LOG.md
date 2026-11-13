# Prompt Change Log

Audit record for every change to model-facing prompt text. Policy: prompts
live in `src/main/resources/prompts/`; any edit appends an entry here —
silent prompt changes are a defined failure mode for this project.

| Date | File | Change | Reason |
|---|---|---|---|
| 2026-11-13 | `fraud-system.txt` | Initial contract: triage-assistant role, never fabricate, list missing info, `humanReviewRequired=true`, no hidden CoT, schema-only output | Capstone §3.5/§6 prompt contract |
| 2026-11-13 | `fraud-user.txt` | Initial template: case summary, signals, evidence, policy passages, conversation, goal variables | §6.2 user-prompt variables |
| 2026-11-13 | `fraud-system-lc4j.txt` | LangChain4j variant of the system contract (AiService `@SystemMessage`) | Same contract, framework-specific load |
| 2026-11-13 | `fraud-followup-system.txt` | Follow-up assistant: answer only from this case's recorded evidence/citations/memory; state limits | Follow-up endpoint grounding |

## Invariants to preserve in future edits

- Role is a **triage assistant**, never a decision-maker.
- Only intake facts, tool outputs, retrieved passages, and case-scoped
  conversation may be used — no outside knowledge, no fabrication.
- Output must be only the agreed `recommendation.schema.json` shape.
- `humanReviewRequired` must always be requested true.
- Ask for a concise evidence-linked rationale — never chain-of-thought.
- Missing/contradictory information must be listed, lowering confidence.
