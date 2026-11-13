package com.capstone.fraudcase.knowledge;

/** One retrievable chunk of a synthetic policy document with full metadata. */
public record PolicyChunk(
        String chunkId,
        String documentId,
        String title,
        String version,
        String effectiveDate,
        String owner,
        String section,
        boolean withdrawn,
        String text) {
}
