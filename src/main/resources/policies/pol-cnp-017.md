---
documentId: POL-CNP-017
title: Card-Not-Present Dispute Handling Procedure
version: "3.2"
effectiveDate: 2026-10-01
owner: Payment Fraud Operations
classification: SYNTHETIC_TRAINING
status: ACTIVE
---

## 3.1 Scope
This procedure covers card-not-present (CNP) disputes where the customer reports a transaction they do not recognise, a purchase made online or by phone. It applies to posted and pending card transactions reported through any intake channel. All identifiers in this training pack are synthetic and masked.

## 3.2 Initial fraud routing
When a customer denies a card-not-present transaction and independent signals show a new or untrusted device, an anomalous location inconsistent with recent trusted activity, or a missing authentication outcome, the case is triaged as a suspected unauthorised transaction. Recommended route: FRAUD_INVESTIGATION with high priority. The assistant may recommend the route only; no card block, refund or customer contact may be performed automatically.

## 3.3 Evidence requirements for unauthorised-use classification
At minimum: (a) the customer's denial statement, (b) transaction details confirming posted status and channel, and (c) at least two independent anomaly signals such as device novelty, impossible-travel distance, authentication failure, or merchant risk. If the authentication challenge outcome is unavailable, record it as missing information rather than inferring it.

## 3.4 Merchant-recognised disputes
Where the customer recognises the merchant but disputes fulfilment, cancellation or the charged amount, the case is not treated as unauthorised use. Route to DISPUTE_OPERATIONS and capture what outcome the customer expects.

## 3.5 Abstention
If no applicable policy passage can be retrieved, or evidence is contradictory, do not invent a procedure. Record the gap and route to MANUAL_REVIEW.
