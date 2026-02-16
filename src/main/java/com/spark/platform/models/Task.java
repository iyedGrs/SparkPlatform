package com.spark.platform.models;

import java.sql.Timestamp;

public class Task {
    private int taskId;
    private int projectId;
    private Integer sprintId;
    private String title;
    private String description;
    private Integer assignedTo;
    private String columnName;
    private String priority;
    private Float estimatedHours;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Task() {}

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public Integer getSprintId() { return sprintId; }
    public void setSprintId(Integer sprintId) { this.sprintId = sprintId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Float getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Float estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Task{" + taskId + ", " + title + ", " + status + "}";
    }
}