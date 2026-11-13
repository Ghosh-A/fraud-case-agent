---
documentId: POL-SLA-002
title: Review Routing SLA and Escalation Targets
version: "1.0"
effectiveDate: 2026-07-20
owner: Case Management Office
classification: SYNTHETIC_TRAINING
status: ACTIVE
---

## 1.1 Queue targets
FRAUD_INVESTIGATION cases carry a 24-hour first-touch target. PAYMENT_OPERATIONS carry 48 hours. DISPUTE_OPERATIONS carry 72 hours. MANUAL_REVIEW cases are worked within 24 hours of arrival.

## 1.2 Priority
Priority is raised to HIGH when two or more independent high-severity anomaly signals exist, or when the disputed amount exceeds the standard review band. Contradictory evidence keeps priority at MEDIUM pending reviewer judgement.

## 1.3 Human control
Every automated recommendation requires a human disposition before a case closes or routes. The reviewer must record a reason; the disposition is appended to the audit trail.
