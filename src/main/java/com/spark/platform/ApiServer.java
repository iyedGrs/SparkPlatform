package com.spark.platform;

import com.spark.platform.controllers.ClassroomController;
import com.spark.platform.controllers.CourseController;
import com.spark.platform.controllers.SessionController;
import com.spark.platform.models.Classroom;
import com.spark.platform.models.Course;
import com.spark.platform.models.Session;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP API server (pure Java, no Spring).
 * Exposes GET endpoints:
 * - /api/classrooms
 * - /api/classrooms/{id}
 * - /api/courses
 * - /api/courses/{id}
 * - /api/sessions
 * - /api/sessions/{id}
 */
public class ApiServer {

    private final ClassroomController classroomController = new ClassroomController();
    private final CourseController courseController = new CourseController();
    private final SessionController sessionController = new SessionController();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        ApiServer apiServer = new ApiServer();
        apiServer.start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/health", this::handleHealth);
        server.createContext("/api/classrooms", this::handleClassrooms);
        server.createContext("/api/courses", this::handleCourses);
        server.createContext("/api/sessions", this::handleSessions);

        server.start();
        System.out.println("API server started on http://localhost:" + port);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendMethodNotAllowed(exchange);
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleClassrooms(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Integer id;
        try {
            id = extractId(path, "/api/classrooms");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid classroom id\"}");
            return;
        }
        if (id == null) {
            List<Classroom> classrooms = classroomController.getAllClassrooms();
            sendJson(exchange, 200, classroomsToJson(classrooms));
            return;
        }

        Optional<Classroom> classroom = classroomController.getClassroomById(id);
        if (classroom.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"Classroom not found\"}");
            return;
        }
        sendJson(exchange, 200, classroomToJson(classroom.get()));
    }

    private void handleCourses(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Integer id;
        try {
            id = extractId(path, "/api/courses");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid course id\"}");
            return;
        }
        if (id == null) {
            List<Course> courses = courseController.getAllCourses();
            sendJson(exchange, 200, coursesToJson(courses));
            return;
        }

        Optional<Course> course = courseController.getCourseById(id);
        if (course.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"Course not found\"}");
            return;
        }
        sendJson(exchange, 200, courseToJson(course.get()));
    }

    private void handleSessions(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Integer id;
        try {
            id = extractId(path, "/api/sessions");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid session id\"}");
            return;
        }
        if (id == null) {
            List<Session> sessions = sessionController.getAllSessions();
            sendJson(exchange, 200, sessionsToJson(sessions));
            return;
        }

        Optional<Session> session = sessionController.getSessionById(id);
        if (session.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"Session not found\"}");
            return;
        }
        sendJson(exchange, 200, sessionToJson(session.get()));
    }

    private boolean isMethod(HttpExchange exchange, String expectedMethod) {
        return expectedMethod.equalsIgnoreCase(exchange.getRequestMethod());
    }

    private Integer extractId(String path, String basePath) {
        if (path.equals(basePath) || path.equals(basePath + "/")) {
            return null;
        }

        if (!path.startsWith(basePath + "/")) {
            throw new IllegalArgumentException("Invalid path");
        }

        String idPart = path.substring(basePath.length() + 1);
        if (idPart.isEmpty() || idPart.contains("/")) {
            throw new IllegalArgumentException("Invalid id format");
        }

        try {
            return Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid id value");
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String classroomsToJson(List<Classroom> classrooms) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < classrooms.size(); i++) {
            sb.append(classroomToJson(classrooms.get(i)));
            if (i < classrooms.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String classroomToJson(Classroom c) {
        return "{"
                + "\"classroomId\":" + c.getClassroomId() + ","
                + "\"name\":" + toJsonString(c.getName()) + ","
                + "\"capacity\":" + c.getCapacity() + ","
                + "\"status\":" + toJsonString(c.getStatus()) + ","
                + "\"createdAt\":" + toJsonString(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                + "}";
    }

    private String coursesToJson(List<Course> courses) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < courses.size(); i++) {
            sb.append(courseToJson(courses.get(i)));
            if (i < courses.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String courseToJson(Course c) {
        return "{"
                + "\"courseId\":" + c.getCourseId() + ","
                + "\"title\":" + toJsonString(c.getTitle()) + ","
                + "\"code\":" + toJsonString(c.getCode()) + ","
                + "\"totalHours\":" + c.getTotalHours() + ","
                + "\"hoursCompleted\":" + c.getHoursCompleted() + ","
                + "\"coefficient\":" + c.getCoefficient() + ","
                + "\"ccWeight\":" + c.getCcWeight() + ","
                + "\"tpWeight\":" + c.getTpWeight() + ","
                + "\"examWeight\":" + c.getExamWeight() + ","
                + "\"semester\":" + (c.getSemester() == null ? "null" : c.getSemester()) + ","
                + "\"status\":" + toJsonString(c.getStatus()) + ","
                + "\"createdAt\":" + toJsonString(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                + "}";
    }

    private String sessionsToJson(List<Session> sessions) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sessions.size(); i++) {
            sb.append(sessionToJson(sessions.get(i)));
            if (i < sessions.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String sessionToJson(Session s) {
        return "{"
                + "\"sessionId\":" + s.getSessionId() + ","
                + "\"courseId\":" + s.getCourseId() + ","
                + "\"classroomId\":" + (s.getClassroomId() == null ? "null" : s.getClassroomId()) + ","
                + "\"teacherId\":" + (s.getTeacherId() == null ? "null" : s.getTeacherId()) + ","
                + "\"dayOfWeek\":" + toJsonString(s.getDayOfWeek()) + ","
                + "\"startTime\":" + toJsonString(s.getStartTime() == null ? null : s.getStartTime().toString()) + ","
                + "\"endTime\":" + toJsonString(s.getEndTime() == null ? null : s.getEndTime().toString()) + ","
                + "\"durationHours\":" + s.getDurationHours() + ","
                + "\"sessionDate\":" + toJsonString(s.getSessionDate() == null ? null : s.getSessionDate().toString()) + ","
                + "\"recurring\":" + s.isRecurring() + ","
                + "\"status\":" + toJsonString(s.getStatus()) + ","
                + "\"createdAt\":" + toJsonString(s.getCreatedAt() == null ? null : s.getCreatedAt().toString())
                + "}";
    }

    private String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
