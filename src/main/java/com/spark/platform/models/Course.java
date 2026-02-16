// filepath: c:\Users\louay\Desktop\spark\SparkPlatform\src\main\java\com\spark\platform\models\Course.java
package com.spark.platform.models;

import java.sql.Timestamp;

public class Course {
    private int courseId;
    private String title;
    private String code;
    private float totalHours;
    private float hoursCompleted;
    private float coefficient;
    private float ccWeight;
    private float tpWeight;
    private float examWeight;
    private Integer semester;
    private String status;
    private Timestamp createdAt;

    public Course() {}

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public float getTotalHours() { return totalHours; }
    public void setTotalHours(float totalHours) { this.totalHours = totalHours; }

    public float getHoursCompleted() { return hoursCompleted; }
    public void setHoursCompleted(float hoursCompleted) { this.hoursCompleted = hoursCompleted; }

    public float getHoursRemaining() { return totalHours - hoursCompleted; }

    public float getCoefficient() { return coefficient; }
    public void setCoefficient(float coefficient) { this.coefficient = coefficient; }

    public float getCcWeight() { return ccWeight; }
    public void setCcWeight(float ccWeight) { this.ccWeight = ccWeight; }

    public float getTpWeight() { return tpWeight; }
    public void setTpWeight(float tpWeight) { this.tpWeight = tpWeight; }

    public float getExamWeight() { return examWeight; }
    public void setExamWeight(float examWeight) { this.examWeight = examWeight; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Course{" + courseId + ", " + code + " - " + title + "}";
    }
}
