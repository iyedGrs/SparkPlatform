package com.spark.platform.controllers;

import com.spark.platform.models.Session;
import com.spark.platform.services.SessionService;

import java.util.List;
import java.util.Optional;

public class SessionController {

    private final SessionService sessionService;

    public SessionController() {
        this.sessionService = new SessionService();
    }

    public List<Session> getAllSessions() {
        return sessionService.findAll();
    }

    public Optional<Session> getSessionById(int id) {
        return sessionService.findById(id);
    }

    public Session createSession(Session session) {
        return sessionService.create(session);
    }

    public Optional<Session> updateSession(int id, Session session) {
        session.setSessionId(id);
        boolean updated = sessionService.update(session);
        if (!updated) {
            return Optional.empty();
        }
        return sessionService.findById(id);
    }

    public boolean deleteSession(int id) {
        return sessionService.delete(id);
    }
}
