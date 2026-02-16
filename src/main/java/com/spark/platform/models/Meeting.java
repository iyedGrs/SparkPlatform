package com.spark.platform.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Meeting {
    private int meetingId;
    private int projectId;
    private Integer sprintId;
    private String meetingType;
    private Date meetingDate;
    private Integer durationMin;
    private String notes;
    private String actionItems;
    private String attendeesJson;
    private String signature;
    private String status;
    private Timestamp createdAt;

    public Meeting() {}

    public int getMeetingId() { return meetingId; }
    public void setMeetingId(int meetingId) { this.meetingId = meetingId; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public Integer getSprintId() { return sprintId; }
    public void setSprintId(Integer sprintId) { this.sprintId = sprintId; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public Date getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Date meetingDate) { this.meetingDate = meetingDate; }

    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getActionItems() { return actionItems; }
    public void setActionItems(String actionItems) { this.actionItems = actionItems; }

    public String getAttendeesJson() { return attendeesJson; }
    public void setAttendeesJson(String attendeesJson) { this.attendeesJson = attendeesJson; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Meeting{" + meetingId + ", " + meetingType + ", " + meetingDate + "}";
    }
}