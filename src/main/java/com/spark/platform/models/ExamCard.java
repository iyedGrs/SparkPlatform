package com.spark.platform.models;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class ExamCard {
    private int examCardId;
    private int courseId;
    private Integer classroomId;
    private Integer teacherId;
    private Date triggerDate;
    private Date examDate;
    private Time startTime;
    private Time endTime;
    private float durationHours;
    private String status;
    private Timestamp createdAt;

    public ExamCard() {}

    public int getExamCardId() { return examCardId; }
    public void setExamCardId(int examCardId) { this.examCardId = examCardId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public Integer getClassroomId() { return classroomId; }
    public void setClassroomId(Integer classroomId) { this.classroomId = classroomId; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public Date getTriggerDate() { return triggerDate; }
    public void setTriggerDate(Date triggerDate) { this.triggerDate = triggerDate; }

    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }

    public Time getStartTime() { return startTime; }
    public void setStartTime(Time startTime) { this.startTime = startTime; }

    public Time getEndTime() { return endTime; }
    public void setEndTime(Time endTime) { this.endTime = endTime; }

    public float getDurationHours() { return durationHours; }
    public void setDurationHours(float durationHours) { this.durationHours = durationHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ExamCard{" + examCardId + ", course=" + courseId + ", " + status + "}";
    }
}