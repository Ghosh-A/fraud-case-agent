package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.common.Hashing;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Deterministic, offline embedding: token + bigram features are hashed into a
 * fixed-dimension vector with TF weighting and L2 normalisation. Identical text
 * always produces an identical vector, and lexical overlap yields real cosine
 * signal — enough for thresholded policy retrieval on synthetic documents.
 */
@Component
public class LocalHashEmbeddingModel implements EmbeddingPort {

    private static final int DIMS = 512;
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("unauthorised", "unauthorized"),
            Map.entry("recognised", "recognized"),
            Map.entry("recognizes", "recognized"),
            Map.entry("cancelled", "canceled"),
            Map.entry("cnp", "card-not-present"),
            Map.entry("chargeback", "dispute"));

    @Override
    public float[] embed(String text) {
        float[] vec = new float[DIMS];
        if (text == null || text.isBlank()) {
            return vec;
        }
        List<String> tokens = tokenize(text);
        Map<Integer, Float> counts = new HashMap<>();
        String prev = null;
        for (String tok : tokens) {
            bump(counts, featureIndex(tok), 1.0f);
            if (prev != null) {
                bump(counts, featureIndex(prev + "_" + tok), 0.5f);
            }
            prev = tok;
        }
        for (var e : counts.entrySet()) {
            vec[e.getKey()] = (float) (e.getValue() > 0 ? 1 + Math.log(e.getValue()) : 0);
        }
        double norm = 0;
        for (float v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= norm;
            }
        }
        return vec;
    }

    private void bump(Map<Integer, Float> counts, int idx, float w) {
        counts.merge(idx, w, Float::sum);
    }

    private int featureIndex(String feature) {
        byte[] hash = Hashing.sha256Hex("emb:" + feature).getBytes();
        return Math.abs(ByteBuffer.wrap(hash, 0, 4).getInt()) % DIMS;
    }

    static List<String> tokenize(String text) {
        String[] raw = text.toLowerCase().split("[^a-z0-9]+");
        java.util.List<String> out = new java.util.ArrayList<>(raw.length);
        for (String t : raw) {
            if (t.length() >= 3) {
                out.add(SYNONYMS.getOrDefault(t, t));
            }
        }
        return out;
    }
}
