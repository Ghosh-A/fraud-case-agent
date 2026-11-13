package com.capstone.fraudcase.agent.common;

/** Exact policy citation metadata — document, version, section, chunk and score. */
public class PolicyCitationRef {

    private String documentId;
    private String version;
    private String section;
    private String chunkId;
    private double score;
    private String passage;

    public PolicyCitationRef() {
    }

    public PolicyCitationRef(String documentId, String version, String section,
                             String chunkId, double score, String passage) {
        this.documentId = documentId;
        this.version = version;
        this.section = section;
        this.chunkId = chunkId;
        this.score = score;
        this.passage = passage;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getPassage() {
        return passage;
    }

    public void setPassage(String passage) {
        this.passage = passage;
    }
}
