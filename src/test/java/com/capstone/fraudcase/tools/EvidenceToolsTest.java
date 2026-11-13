package com.capstone.fraudcase.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.AuditEventWriter;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignalsResult;
import com.capstone.fraudcase.tools.ToolResults.SlaResult;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetailsResult;
import com.capstone.fraudcase.domain.ToolStatus;
import org.junit.jupiter.api.Test;

class EvidenceToolsTest {

    private final EvidenceTools tools = new EvidenceTools(
            new DeterministicFixtureGenerator(new CapstoneProperties()),
            mock(PolicyRetriever.class),
            mock(AuditEventWriter.class));

    @Test
    void transactionDetailsDeterministic() {
        TransactionDetailsResult a = tools.getTransactionDetails("TXN-1", "CUST-A");
        TransactionDetailsResult b = tools.getTransactionDetails("TXN-1", "CUST-A");
        assertThat(a.isFound()).isEqualTo(b.isFound());
        assertThat(a.getAuthOutcome()).isEqualTo(b.getAuthOutcome());
        assertThat(a.getChannel()).isEqualTo(b.getChannel());
    }

    @Test
    void missingTransactionReturnsNotFound() {
        TransactionDetailsResult r = tools.getTransactionDetails("TXN-SCN-MISSING-9", "CUST-A");
        assertThat(r.isFound()).isFalse();
        assertThat(r.getStatus()).isEqualTo(ToolStatus.NOT_FOUND);
        assertThat(r.getUnavailableReason()).contains("not found");
    }

    @Test
    void unauthScenarioShowsNewDevice() {
        DeviceSignalsResult r = tools.getDeviceSignals("CUST-A", "TXN-SCN-UNAUTH-1");
        assertThat(r.isDeviceKnown()).isFalse();
        assertThat(r.getTrustLevel()).isEqualTo("LOW");
    }

    @Test
    void slaRoutesToQueues() {
        SlaResult fraud = tools.calculateCaseSla("FRAUD_INVESTIGATION");
        assertThat(fraud.getQueue()).isEqualTo("FRAUD-OPS");
        SlaResult manual = tools.calculateCaseSla("MANUAL_REVIEW");
        assertThat(manual.getQueue()).isEqualTo("MANUAL-REVIEW");
    }

    @Test
    void everyResultCarriesProvenance() {
        TransactionDetailsResult r = tools.getTransactionDetails("TXN-1", "CUST-A");
        assertThat(r.getToolResultId()).isNotBlank();
        assertThat(r.getObservedAt()).isNotBlank();
        assertThat(r.getOrigin()).isEqualTo("synthetic-fixture");
    }
}
