---
documentId: POL-DSP-004
title: Merchant Dispute Operations Guide
version: "1.7"
effectiveDate: 2026-08-15
owner: Dispute Operations
classification: SYNTHETIC_TRAINING
status: ACTIVE
---

## 1.1 Scope
This guide covers disputes where the customer recognises the merchant but contests the goods, services, cancellation terms or charged amount. These are commercial disputes, not unauthorised use.

## 1.2 Fulfilment and cancellation complaints
When the customer reports non-delivery, partial delivery, service not rendered, or a cancelled recurring charge that still posted, classify as MERCHANT_DISPUTE. Route to DISPUTE_OPERATIONS. Capture the expected resolution and any merchant contact the customer already attempted.

## 1.3 Amount disputes
Where the recognised merchant charged a different amount than agreed, the dispute remains a merchant dispute. Verify the claimed amount against the posted amount in transaction details before routing.

## 1.4 Escalation to fraud review
Escalate to FRAUD_INVESTIGATION only when independent signals (device novelty, location anomaly, authentication failure) suggest the customer did not author the transaction despite recognising the merchant name.
