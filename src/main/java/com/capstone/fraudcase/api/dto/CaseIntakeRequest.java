package com.capstone.fraudcase.api.dto;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public class CaseIntakeRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9 .•*\\-_]{4,64}$", message = "masked customer reference required")
    private String maskedCustomerRef;

    @Size(max = 64)
    private String homeRegion;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9 .•*\\-_]{4,64}$", message = "masked transaction reference required")
    private String transactionRef;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "ISO-4217 currency code required")
    private String currency;

    @NotNull
    private Instant occurredAt;

    @NotNull
    private TransactionStatus status;

    @NotNull
    private TransactionChannel channel;

    @NotBlank
    @Size(max = 96)
    private String maskedMerchant;

    @NotNull
    private IssueType issueType;

    @NotBlank
    @Size(max = 2000)
    private String customerStatement;

    @NotNull
    private CardPossession cardPossession;

    @NotNull
    private Boolean merchantRecognised;

    @NotNull
    private IntakeChannel intakeChannel = IntakeChannel.WEB;

    @NotNull
    private Instant reportedAt;

    @AssertTrue(message = "synthetic data confirmation is required")
    private boolean syntheticDataConfirmed;

    @AssertTrue(message = "human review must remain required")
    private boolean reviewRequired;

    public String getMaskedCustomerRef() {
        return maskedCustomerRef;
    }

    public void setMaskedCustomerRef(String maskedCustomerRef) {
        this.maskedCustomerRef = maskedCustomerRef;
    }

    public String getHomeRegion() {
        return homeRegion;
    }

    public void setHomeRegion(String homeRegion) {
        this.homeRegion = homeRegion;
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionChannel getChannel() {
        return channel;
    }

    public void setChannel(TransactionChannel channel) {
        this.channel = channel;
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

    public Boolean getMerchantRecognised() {
        return merchantRecognised;
    }

    public void setMerchantRecognised(Boolean merchantRecognised) {
        this.merchantRecognised = merchantRecognised;
    }

    public IntakeChannel getIntakeChannel() {
        return intakeChannel;
    }

    public void setIntakeChannel(IntakeChannel intakeChannel) {
        this.intakeChannel = intakeChannel;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public boolean isSyntheticDataConfirmed() {
        return syntheticDataConfirmed;
    }

    public void setSyntheticDataConfirmed(boolean syntheticDataConfirmed) {
        this.syntheticDataConfirmed = syntheticDataConfirmed;
    }

    public boolean isReviewRequired() {
        return reviewRequired;
    }

    public void setReviewRequired(boolean reviewRequired) {
        this.reviewRequired = reviewRequired;
    }
}
