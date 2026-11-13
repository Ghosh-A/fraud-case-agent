package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Validated intake facts handed to a processor — masked identifiers only. */
public record CaseSummary(
        String caseId,
        String maskedCustomerRef,
        String homeRegion,
        String transactionRef,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        TransactionStatus transactionStatus,
        TransactionChannel transactionChannel,
        String maskedMerchant,
        IssueType issueType,
        String customerStatement,
        CardPossession cardPossession,
        boolean merchantRecognised,
        Instant reportedAt) {

    public String toPromptText() {
        return "caseId=" + caseId
                + "; customer=" + maskedCustomerRef + " (region " + homeRegion + ")"
                + "; transaction=" + transactionRef + " " + amount + " " + currency
                + " at " + occurredAt + " [" + transactionStatus + "/" + transactionChannel + "]"
                + "; merchant=" + maskedMerchant
                + "; issueType=" + issueType
                + "; cardPossession=" + cardPossession
                + "; merchantRecognised=" + merchantRecognised
                + "; statement=\"" + customerStatement + "\"";
    }
}
