package com.capstone.fraudcase.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyIngestionServiceTest {

    private final InMemoryPolicyVectorStore store = new InMemoryPolicyVectorStore();
    private final LocalHashEmbeddingModel embedding = new LocalHashEmbeddingModel();
    private final PolicyIngestionService svc = new PolicyIngestionService(store, embedding);

    private static final String DOC = """
            ---
            documentId: POL-TEST-1
            title: Test Policy
            version: "1.0"
            effectiveDate: 2026-01-01
            owner: Test Owner
            status: ACTIVE
            ---

            ## 1.1 First section
            Dispute handling for card-not-present transactions requires evidence.
            ## 1.2 Second section
            Fraud investigation routing follows anomaly severity.
            """;

    @Test
    void parsesHeaderMetadata() {
        PolicyDocumentMeta meta = PolicyIngestionService.parseMeta("""
                documentId: POL-X
                title: Doc
                version: "2.0"
                effectiveDate: 2026-01-01
                owner: Ops
                """);
        assertThat(meta.documentId()).isEqualTo("POL-X");
        assertThat(meta.version()).isEqualTo("2.0");
        assertThat(meta.withdrawn()).isFalse();
    }

    @Test
    void chunksByHeadingsAndStoresEmbeddings() {
        List<PolicyChunk> chunks = svc.ingestText("t.md", DOC);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).section()).isEqualTo("1.1 First section");
        assertThat(chunks.get(0).chunkId()).isEqualTo("pol-test-1-c0");
        assertThat(chunks.get(1).chunkId()).isEqualTo("pol-test-1-c1");
        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void missingHeaderRejected() {
        assertThatThrownBy(() -> svc.ingestText("bad.md", "no header"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawnDocsFlagged() {
        PolicyDocumentMeta meta = new PolicyDocumentMeta("P", "t", "1", "2026-01-01", "o",
                "SYNTHETIC_TRAINING", "WITHDRAWN");
        assertThat(meta.withdrawn()).isTrue();
    }
}
