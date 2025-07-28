package com.redhat.chatbot.model;

import java.time.LocalDateTime;

public class ChatMessage {

    private String id;

    private String content;

    private String role; // "user" or "assistant"

    private LocalDateTime timestamp;

    public ChatMessage() {

        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String content, String role) {

        this();
        this.content = content;
        this.role = role;
    }

    // Getters and setters
    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getContent() {

        return content;
    }

    public void setContent(String content) {

        this.content = content;
    }

    public String getRole() {

        return role;
    }

    public void setRole(String role) {

        this.role = role;
    }

    public LocalDateTime getTimestamp() {

        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {

        this.timestamp = timestamp;
    }

}