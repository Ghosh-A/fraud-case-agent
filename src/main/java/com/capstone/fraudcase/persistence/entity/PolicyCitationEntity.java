package com.capstone.fraudcase.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_citation")
public class PolicyCitationEntity {

    @Id
    @Column(name = "citation_id", length = 48)
    private String citationId;

    @Column(name = "recommendation_id", nullable = false, length = 48)
    private String recommendationId;

    @Column(name = "case_id", nullable = false, length = 40)
    private String caseId;

    @Column(name = "document_id", nullable = false, length = 64)
    private String documentId;

    @Column(name = "doc_version", nullable = false, length = 16)
    private String version;

    @Column(name = "section", length = 64)
    private String section;

    @Column(name = "chunk_id", nullable = false, length = 64)
    private String chunkId;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "passage", nullable = false, length = 2000)
    private String passage;

    public String getCitationId() {
        return citationId;
    }

    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
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
