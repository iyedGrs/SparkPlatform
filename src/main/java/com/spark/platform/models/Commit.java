package com.spark.platform.models;

import java.time.LocalDateTime;

public class Commit {
    private int commitId;
    private int projectId;
    private Integer taskId;
    private Integer userId;
    private String commitHash;
    private String author;
    private String message;
    private int additions;
    private int deletions;
    private LocalDateTime committedAt;
    private String status;

    public Commit() {}

    public int getCommitId() { return commitId; }
    public void setCommitId(int commitId) { this.commitId = commitId; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }

    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }

    public LocalDateTime getCommittedAt() { return committedAt; }
    public void setCommittedAt(LocalDateTime committedAt) { this.committedAt = committedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Commit{" + commitHash.substring(0, 7) + ", " + message + "}";
    }
}