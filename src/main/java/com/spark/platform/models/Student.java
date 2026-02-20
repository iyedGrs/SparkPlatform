package com.spark.platform.models;

public class Student extends User {

    public Student() {
        setUserType("STUDENT");
        setStatus("ACTIVE");
    }

    public Student(String name, String email) {
        this();
        setName(name);
        setEmail(email);
    }
}
