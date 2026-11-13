package com.capstone.fraudcase.knowledge;

/** Parsed synthetic policy document header. */
public record PolicyDocumentMeta(
        String documentId,
        String title,
        String version,
        String effectiveDate,
        String owner,
        String classification,
        String status) {

    public boolean withdrawn() {
        return "WITHDRAWN".equalsIgnoreCase(status);
    }
}
