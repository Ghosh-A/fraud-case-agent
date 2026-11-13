package com.capstone.fraudcase.knowledge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Ingests synthetic policy documents: validates header metadata, splits by
 * headings, embeds chunks and loads them into the vector store. Chunks from
 * withdrawn documents are stored but never retrieved.
 */
@Service
public class PolicyIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);
    private static final int MAX_CHUNK_CHARS = 1200;

    private final PolicyVectorStore vectorStore;
    private final EmbeddingPort embedding;
    private final List<PolicyDocumentMeta> documents = new ArrayList<>();

    public PolicyIngestionService(PolicyVectorStore vectorStore, EmbeddingPort embedding) {
        this.vectorStore = vectorStore;
        this.embedding = embedding;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Resource res : policyResources()) {
            ingest(res);
        }
        log.info("Policy ingestion complete: {} document(s), {} chunk(s)",
                documents.size(), vectorStore.size());
    }

    public List<PolicyDocumentMeta> documents() {
        return List.copyOf(documents);
    }

    private Resource[] policyResources() {
        try {
            return new PathMatchingResourcePatternResolver().getResources("classpath:policies/*.md");
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list policy resources", e);
        }
    }

    void ingest(Resource resource) {
        try {
            String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ingestText(resource.getFilename(), text);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read policy " + resource.getFilename(), e);
        }
    }

    /** Split header/body for testability. */
    List<PolicyChunk> ingestText(String filename, String text) {
        String[] halves = text.split("(?m)^---\\s*$", -1);
        if (halves.length < 3) {
            throw new IllegalArgumentException("policy file " + filename + " lacks --- header markers");
        }
        PolicyDocumentMeta meta = parseMeta(halves[1]);
        String body = halves[2];
        List<PolicyChunk> chunks = chunk(meta, body);
        for (PolicyChunk c : chunks) {
            vectorStore.add(c, embedding.embed(c.text()));
        }
        documents.add(meta);
        return chunks;
    }

    static PolicyDocumentMeta parseMeta(String header) {
        Map<String, String> kv = new HashMap<>();
        for (String line : header.split("\\R")) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                kv.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim().replace("\"", ""));
            }
        }
        PolicyDocumentMeta meta = new PolicyDocumentMeta(
                require(kv, "documentId"), require(kv, "title"), require(kv, "version"),
                require(kv, "effectiveDate"), require(kv, "owner"),
                kv.getOrDefault("classification", "SYNTHETIC_TRAINING"),
                kv.getOrDefault("status", "ACTIVE"));
        return meta;
    }

    private static String require(Map<String, String> kv, String key) {
        String v = kv.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("policy header missing " + key);
        }
        return v;
    }

    /** Split on markdown headings so a rule is not broken across unrelated chunks. */
    static List<PolicyChunk> chunk(PolicyDocumentMeta meta, String body) {
        List<PolicyChunk> out = new ArrayList<>();
        String[] blocks = body.split("(?m)(?=^#{2,3} )");
        int idx = 0;
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String section = trimmed.lines().findFirst()
                    .map(l -> l.replaceFirst("^#{2,3}\\s*", "").trim()).orElse("body");
            for (String piece : splitLong(trimmed, MAX_CHUNK_CHARS)) {
                String chunkId = meta.documentId().toLowerCase() + "-c" + (idx++);
                out.add(new PolicyChunk(chunkId, meta.documentId(), meta.title(), meta.version(),
                        meta.effectiveDate(), meta.owner(), section, meta.withdrawn(), piece));
            }
        }
        return out;
    }

    private static List<String> splitLong(String text, int max) {
        if (text.length() <= max) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            parts.add(text.substring(start, Math.min(start + max, text.length())));
            start += max;
        }
        return parts;
    }
}
