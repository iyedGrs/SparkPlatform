package com.spark.platform.models;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class Session {
    private int sessionId;
    private int courseId;
    private Integer classroomId;
    private Integer teacherId;
    private String dayOfWeek;
    private Time startTime;
    private Time endTime;
    private float durationHours;
    private Date sessionDate;
    private boolean recurring;
    private String status;
    private Timestamp createdAt;

    public Session() {}

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public Integer getClassroomId() { return classroomId; }
    public void setClassroomId(Integer classroomId) { this.classroomId = classroomId; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Time getStartTime() { return startTime; }
    public void setStartTime(Time startTime) { this.startTime = startTime; }

    public Time getEndTime() { return endTime; }
    public void setEndTime(Time endTime) { this.endTime = endTime; }

    public float getDurationHours() { return durationHours; }
    public void setDurationHours(float durationHours) { this.durationHours = durationHours; }

    public Date getSessionDate() { return sessionDate; }
    public void setSessionDate(Date sessionDate) { this.sessionDate = sessionDate; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Session{" + sessionId + ", " + dayOfWeek + " " + startTime + "-" + endTime + "}";
    }
}