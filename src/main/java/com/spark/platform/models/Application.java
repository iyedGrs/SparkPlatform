package com.spark.platform.models;

import java.sql.Timestamp;

public class Application {
    private int applicationId;
    private int jobId;
    private int studentId;
    private Float matchScore;
    private String coverNote;
    private String resumePath;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Application() {}

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public Float getMatchScore() { return matchScore; }
    public void setMatchScore(Float matchScore) { this.matchScore = matchScore; }

    public String getCoverNote() { return coverNote; }
    public void setCoverNote(String coverNote) { this.coverNote = coverNote; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Application{" + applicationId + ", job=" + jobId + ", student=" + studentId + ", match=" + matchScore + "%}";
    }
}