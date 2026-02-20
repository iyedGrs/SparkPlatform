package com.spark.platform.models;

public class Teacher extends User {

    public Teacher() {
        setUserType("TEACHER");
        setStatus("ACTIVE");
    }

    public Teacher(String name, String email) {
        this();
        setName(name);
        setEmail(email);
    }
}
