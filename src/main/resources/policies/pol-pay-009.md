---
documentId: POL-PAY-009
title: Duplicate Processing and Payment Operations Procedure
version: "2.1"
effectiveDate: 2026-09-01
owner: Payment Operations
classification: SYNTHETIC_TRAINING
status: ACTIVE
---

## 2.1 Identifying duplicate processing
A duplicate processing claim exists when the same payment fingerprint posts more than once, or matching transaction records share amount, merchant and timestamp. Confirm via transaction details before routing.

## 2.2 Routing
Confirmed duplicate postings route to PAYMENT_OPERATIONS. The recommended route is advisory; correction of a posting is performed only by an authorised operations reviewer.

## 2.3 Distinguishing duplicates from repeat fraud
If the customer denies BOTH postings, treat as potential unauthorised use instead and route to FRAUD_INVESTIGATION. Prior dispute history should be checked to see whether earlier claims were resolved as duplicates.
