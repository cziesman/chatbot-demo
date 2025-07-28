package com.redhat.chatbot.model;

public class SearchResult {

    private String content;

    private float score;

    private String documentId;

    private String filename;

    public SearchResult(String content, float score, String documentId, String filename) {

        this.content = content;
        this.score = score;
        this.documentId = documentId;
        this.filename = filename;
    }

    // Getters and setters
    public String getContent() {

        return content;
    }

    public void setContent(String content) {

        this.content = content;
    }

    public float getScore() {

        return score;
    }

    public void setScore(float score) {

        this.score = score;
    }

    public String getDocumentId() {

        return documentId;
    }

    public void setDocumentId(String documentId) {

        this.documentId = documentId;
    }

    public String getFilename() {

        return filename;
    }

    public void setFilename(String filename) {

        this.filename = filename;
    }

}