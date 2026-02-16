package com.spark.platform.models;

import java.sql.Timestamp;

public class Classroom {
    private int classroomId;
    private String name;
    private int capacity;
    private String status;
    private Timestamp createdAt;

    public Classroom() {}

    public Classroom(int classroomId, String name, int capacity) {
        this.classroomId = classroomId;
        this.name = name;
        this.capacity = capacity;
        this.status = "ACTIVE";
    }

    public int getClassroomId() { return classroomId; }
    public void setClassroomId(int classroomId) { this.classroomId = classroomId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Classroom{" + classroomId + ", " + name + "}";
    }
}