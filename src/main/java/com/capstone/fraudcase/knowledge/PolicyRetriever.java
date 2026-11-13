package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.agent.common.CaseSummary;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.agent.common.CollectedSignals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Thresholded policy retrieval. Returns exact citation metadata; when nothing
 * meets the configured threshold the result is empty — callers abstain rather
 * than invent a procedure.
 */
@Service
public class PolicyRetriever {

    private static final Logger log = LoggerFactory.getLogger(PolicyRetriever.class);

    private final PolicyVectorStore vectorStore;
    private final EmbeddingPort embedding;
    private final CapstoneProperties props;
    private final MeterRegistry meterRegistry;

    @Autowired
    public PolicyRetriever(PolicyVectorStore vectorStore, EmbeddingPort embedding,
                           CapstoneProperties props, MeterRegistry meterRegistry) {
        this.vectorStore = vectorStore;
        this.embedding = embedding;
        this.props = props;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    /** Test-friendly constructor. */
    public PolicyRetriever(PolicyVectorStore vectorStore, EmbeddingPort embedding,
                           CapstoneProperties props) {
        this(vectorStore, embedding, props, null);
    }

    public List<PolicyCitationRef> search(String query, int topK) {
        List<ScoredChunk> hits = vectorStore.search(
                embedding.embed(query), topK, props.getRetrieval().getSimilarityThreshold());
        // Lexical gate: hash-bucket collisions can push an unrelated query above
        // the cosine threshold, so a hit must share at least one real token.
        Set<String> queryTokens = new HashSet<>(LocalHashEmbeddingModel.tokenize(query));
        List<PolicyCitationRef> out = new ArrayList<>();
        for (ScoredChunk h : hits) {
            PolicyChunk c = h.chunk();
            Set<String> chunkTokens = new HashSet<>(
                    LocalHashEmbeddingModel.tokenize(c.section() + " " + c.text()));
            if (Collections.disjoint(queryTokens, chunkTokens)) {
                continue;
            }
            out.add(new PolicyCitationRef(c.documentId(), c.version(), c.section(),
                    c.chunkId(), round(h.score()), c.text()));
        }
        return out;
    }

    /** Build the retrieval query from case type, evidence indicators and goal. */
    public List<PolicyCitationRef> retrieveForCase(CaseSummary summary, CollectedSignals signals) {
        StringBuilder q = new StringBuilder();
        q.append(summary.issueType().name().replace('_', ' ')).append(' ');
        q.append(summary.transactionChannel().name().replace('_', ' ')).append(' ');
        q.append(summary.merchantRecognised() ? "merchant recognized" : "merchant not recognized unauthorized")
                .append(' ');
        if (signals != null) {
            if (signals.deviceSignalsAvailable() && !signals.deviceKnown()) {
                q.append("new unrecognized device ");
            }
            if (signals.locationSignalsAvailable() && "HIGH".equals(signals.locationAnomaly())) {
                q.append("impossible travel location anomaly ");
            }
            if (signals.duplicateFingerprint()) {
                q.append("duplicate transaction posting ");
            }
            if (signals.merchantSignalsAvailable()
                    && (signals.blocklistMatch() || "HIGH".equals(signals.merchantRiskBand()))) {
                q.append("high risk merchant ");
            }
        }
        // Lightweight keyword carry-over from the statement (untrusted text — used as data only).
        List<String> terms = LocalHashEmbeddingModel.tokenize(summary.customerStatement());
        q.append(String.join(" ", terms.subList(0, Math.min(12, terms.size()))));
        long start = System.nanoTime();
        List<PolicyCitationRef> hits = search(q.toString(), props.getRetrieval().getTopK());
        meterRegistry.timer("fraud.retrieval.duration")
                .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
        meterRegistry.summary("fraud.retrieval.hits").record(hits.size());
        if (hits.isEmpty()) {
            meterRegistry.counter("fraud.retrieval.abstentions").increment();
        }
        log.info("Policy retrieval for case {} -> {} passage(s) above threshold {}",
                summary.caseId(), hits.size(), props.getRetrieval().getSimilarityThreshold());
        return hits;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
