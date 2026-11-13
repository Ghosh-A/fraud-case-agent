package com.capstone.fraudcase.knowledge;

/** Framework-neutral embedding port. Deterministic implementation keeps tests offline. */
public interface EmbeddingPort {

    float[] embed(String text);
}
