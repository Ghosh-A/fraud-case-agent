package com.capstone.fraudcase.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Simple in-memory cosine-similarity store for policy chunks. */
@Component
public class InMemoryPolicyVectorStore implements PolicyVectorStore {

    private record Entry(PolicyChunk chunk, float[] vector) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void add(PolicyChunk chunk, float[] vector) {
        entries.add(new Entry(chunk, vector));
    }

    @Override
    public List<ScoredChunk> search(float[] queryVector, int topK, double threshold) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (Entry e : entries) {
            if (e.chunk().withdrawn()) {
                continue;
            }
            double score = cosine(queryVector, e.vector());
            if (score >= threshold) {
                scored.add(new ScoredChunk(e.chunk(), score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    @Override
    public int size() {
        return entries.size();
    }

    static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
