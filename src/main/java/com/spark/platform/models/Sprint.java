package com.spark.platform.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Sprint {
    private int sprintId;
    private int projectId;
    private int sprintNumber;
    private String title;
    private Date startDate;
    private Date endDate;
    private String goal;
    private String status;
    private Timestamp createdAt;

    public Sprint() {}

    public int getSprintId() { return sprintId; }
    public void setSprintId(int sprintId) { this.sprintId = sprintId; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getSprintNumber() { return sprintNumber; }
    public void setSprintNumber(int sprintNumber) { this.sprintNumber = sprintNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Sprint{" + sprintId + ", #" + sprintNumber + ", " + title + "}";
    }
}