package com.redhat.chatbot.model;

import java.util.List;
import java.util.Map;

public class Document {

    private String id;

    private String filename;

    private String content;

    private List<String> chunks;

    private Map<String, Object> metadata;

    private float[] embedding;

    public Document() {

    }

    public Document(String filename, String content) {

        this.filename = filename;
        this.content = content;
    }

    // Getters and setters
    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getFilename() {

        return filename;
    }

    public void setFilename(String filename) {

        this.filename = filename;
    }

    public String getContent() {

        return content;
    }

    public void setContent(String content) {

        this.content = content;
    }

    public List<String> getChunks() {

        return chunks;
    }

    public void setChunks(List<String> chunks) {

        this.chunks = chunks;
    }

    public Map<String, Object> metadata() {

        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {

        this.metadata = metadata;
    }

    public float[] getEmbedding() {

        return embedding;
    }

    public void setEmbedding(float[] embedding) {

        this.embedding = embedding;
    }

}