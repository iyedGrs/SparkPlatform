package com.spark.platform.utils;

import com.spark.platform.models.User;

/**
 * Singleton to hold the currently logged-in user.
 * Set after successful login, cleared on logout.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return isLoggedIn() && "ADMINISTRATOR".equals(currentUser.getUserType());
    }

    public boolean isTeacher() {
        return isLoggedIn() && "TEACHER".equals(currentUser.getUserType());
    }

    public boolean isStudent() {
        return isLoggedIn() && "STUDENT".equals(currentUser.getUserType());
    }

    public int getUserId() {
        return isLoggedIn() ? currentUser.getUserId() : -1;
    }
}