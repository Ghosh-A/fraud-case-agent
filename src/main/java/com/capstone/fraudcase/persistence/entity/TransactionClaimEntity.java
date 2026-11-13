package com.capstone.fraudcase.persistence.entity;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_claim")
public class TransactionClaimEntity {

    @Id
    @Column(name = "claim_id", length = 40)
    private String claimId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    @Column(name = "transaction_ref", nullable = false, length = 64)
    private String transactionRef;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_status", nullable = false, length = 24)
    private TransactionStatus txnStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_channel", nullable = false, length = 24)
    private TransactionChannel txnChannel;

    @Column(name = "masked_merchant", nullable = false, length = 96)
    private String maskedMerchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 32)
    private IssueType issueType;

    @Column(name = "customer_statement", nullable = false, length = 2000)
    private String customerStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_possession", nullable = false, length = 24)
    private CardPossession cardPossession;

    @Column(name = "merchant_recognised", nullable = false)
    private boolean merchantRecognised;

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public TransactionStatus getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(TransactionStatus txnStatus) {
        this.txnStatus = txnStatus;
    }

    public TransactionChannel getTxnChannel() {
        return txnChannel;
    }

    public void setTxnChannel(TransactionChannel txnChannel) {
        this.txnChannel = txnChannel;
    }

    public String getMaskedMerchant() {
        return maskedMerchant;
    }

    public void setMaskedMerchant(String maskedMerchant) {
        this.maskedMerchant = maskedMerchant;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public String getCustomerStatement() {
        return customerStatement;
    }

    public void setCustomerStatement(String customerStatement) {
        this.customerStatement = customerStatement;
    }

    public CardPossession getCardPossession() {
        return cardPossession;
    }

    public void setCardPossession(CardPossession cardPossession) {
        this.cardPossession = cardPossession;
    }

    public boolean isMerchantRecognised() {
        return merchantRecognised;
    }

    public void setMerchantRecognised(boolean merchantRecognised) {
        this.merchantRecognised = merchantRecognised;
    }
}
