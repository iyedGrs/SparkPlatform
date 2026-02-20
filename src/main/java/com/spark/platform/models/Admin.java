package com.spark.platform.models;

public class Admin extends User {

    public Admin() {
        setUserType("ADMINISTRATOR");
        setStatus("ACTIVE");
    }

    public Admin(String name, String email) {
        this();
        setName(name);
        setEmail(email);
    }
}
