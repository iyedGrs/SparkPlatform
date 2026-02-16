package com.spark.platform.models;

import java.sql.Timestamp;

public class ChatSession {
    private int sessionId;
    private int userId;
    private String title;
    private String status;
    private Timestamp createdAt;

    public ChatSession() {}

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ChatSession{" + sessionId + ", user=" + userId + "}";
    }
}