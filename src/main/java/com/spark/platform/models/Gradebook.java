package com.spark.platform.models;

import java.sql.Timestamp;

public class Gradebook {
    private int gradebookId;
    private int studentId;
    private int courseId;
    private String assessment;
    private Float grade;
    private float maxGrade;
    private float weight;
    private Integer semester;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Gradebook() {}

    public int getGradebookId() { return gradebookId; }
    public void setGradebookId(int gradebookId) { this.gradebookId = gradebookId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getAssessment() { return assessment; }
    public void setAssessment(String assessment) { this.assessment = assessment; }

    public Float getGrade() { return grade; }
    public void setGrade(Float grade) { this.grade = grade; }

    public float getMaxGrade() { return maxGrade; }
    public void setMaxGrade(float maxGrade) { this.maxGrade = maxGrade; }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Gradebook{student=" + studentId + ", course=" + courseId + ", " + assessment + "=" + grade + "}";
    }
}