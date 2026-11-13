package com.capstone.fraudcase.knowledge;

import java.util.List;

/** Vector-store port — provider details stay behind this adapter. */
public interface PolicyVectorStore {

    void add(PolicyChunk chunk, float[] vector);

    /** Chunks with cosine similarity >= threshold, best first, capped at topK. */
    List<ScoredChunk> search(float[] queryVector, int topK, double threshold);

    int size();
}
