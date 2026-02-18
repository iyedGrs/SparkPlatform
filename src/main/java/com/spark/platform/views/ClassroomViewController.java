package com.spark.platform.views;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.controllers.ClassroomController;
import com.spark.platform.controllers.CourseController;
import com.spark.platform.controllers.SessionController;
import com.spark.platform.models.Classroom;
import com.spark.platform.models.Course;
import com.spark.platform.models.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ClassroomViewController {

    private static final List<String> WEEK_DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
    );

    @FXML
    private VBox classesContainer;

    @FXML
    private Label summaryLabel;

    @FXML
    private Label selectionLabel;

    @FXML
    private Button editClassroomButton;

    @FXML
    private Button deleteClassroomButton;

    @FXML
    private Button editCourseButton;

    @FXML
    private Button deleteCourseButton;

    @FXML
    private Button addSessionButton;

    private final DatabaseConfig databaseConfig = DatabaseConfig.getInstance();
    private final ClassroomController classroomController = new ClassroomController();
    private final CourseController courseController = new CourseController();
    private final SessionController sessionController = new SessionController();

    private Map<Integer, ClassroomItem> cachedClassrooms = new LinkedHashMap<>();
    private Integer expandedClassroomId;
    private Integer expandedCourseId;

    @FXML
    private void initialize() {
        loadHierarchy();
    }

    @FXML
    private void onRefreshClick() {
        loadHierarchy();
    }

    @FXML
    private void onAddClassroomClick() {
        Optional<String> name = promptText("Nouvelle classe", "Nom de la classe", "", true);
        if (name.isEmpty()) {
            return;
        }
        Optional<Integer> capacity = promptInt("Nouvelle classe", "Capacite (entier >= 0)", "30", 0);
        if (capacity.isEmpty()) {
            return;
        }
        Optional<String> status = promptText("Nouvelle classe", "Status", "ACTIVE", false);
        if (status.isEmpty()) {
            return;
        }

        Classroom classroom = new Classroom();
        classroom.setName(name.get());
        classroom.setCapacity(capacity.get());
        classroom.setStatus(normalizeOrDefault(status.get(), "ACTIVE"));

        try {
            Classroom created = classroomController.createClassroom(classroom);
            expandedClassroomId = created.getClassroomId();
            expandedCourseId = null;
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Creation impossible", e.getMessage());
        }
    }

    @FXML
    private void onEditClassroomClick() {
        if (expandedClassroomId == null) {
            showWarning("Selection requise", "Selectionnez une classe.");
            return;
        }

        try {
            Optional<Classroom> classroomOptional = classroomController.getClassroomById(expandedClassroomId);
            if (classroomOptional.isEmpty()) {
                showWarning("Introuvable", "La classe n'existe plus.");
                loadHierarchy();
                return;
            }

            Classroom current = classroomOptional.get();
            Optional<String> name = promptText("Modifier classe", "Nom de la classe", current.getName(), true);
            if (name.isEmpty()) {
                return;
            }
            Optional<Integer> capacity = promptInt(
                    "Modifier classe",
                    "Capacite (entier >= 0)",
                    String.valueOf(current.getCapacity()),
                    0
            );
            if (capacity.isEmpty()) {
                return;
            }
            Optional<String> status = promptText(
                    "Modifier classe",
                    "Status",
                    normalizeOrDefault(current.getStatus(), "ACTIVE"),
                    false
            );
            if (status.isEmpty()) {
                return;
            }

            current.setName(name.get());
            current.setCapacity(capacity.get());
            current.setStatus(normalizeOrDefault(status.get(), "ACTIVE"));
            classroomController.updateClassroom(current.getClassroomId(), current);
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    @FXML
    private void onDeleteClassroomClick() {
        if (expandedClassroomId == null) {
            showWarning("Selection requise", "Selectionnez une classe.");
            return;
        }

        if (!showConfirmation(
                "Supprimer classe",
                "Supprimer la classe #" + expandedClassroomId + " ?",
                "Les seances associees perdront la reference de classe."
        )) {
            return;
        }

        try {
            classroomController.deleteClassroom(expandedClassroomId);
            expandedClassroomId = null;
            expandedCourseId = null;
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    @FXML
    private void onAddCourseClick() {
        Optional<String> title = promptText("Nouveau cours", "Titre du cours", "", true);
        if (title.isEmpty()) {
            return;
        }
        Optional<String> code = promptText("Nouveau cours", "Code du cours", "", false);
        if (code.isEmpty()) {
            return;
        }
        Optional<Float> totalHours = promptFloat("Nouveau cours", "Total heures (>= 0)", "0", 0f);
        if (totalHours.isEmpty()) {
            return;
        }
        Optional<Integer> semester = promptOptionalInt("Nouveau cours", "Semestre (optionnel)", "");
        if (semester.isEmpty()) {
            return;
        }
        Optional<String> status = promptText("Nouveau cours", "Status", "ACTIVE", false);
        if (status.isEmpty()) {
            return;
        }

        Course course = new Course();
        course.setTitle(title.get());
        course.setCode(normalizeOrNull(code.get()));
        course.setTotalHours(totalHours.get());
        course.setHoursCompleted(0f);
        course.setCoefficient(1f);
        course.setCcWeight(0.25f);
        course.setTpWeight(0.25f);
        course.setExamWeight(0.5f);
        course.setSemester(optionalIntOrNull(semester.get()));
        course.setStatus(normalizeOrDefault(status.get(), "ACTIVE"));

        try {
            courseController.createCourse(course);
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Creation impossible", e.getMessage());
        }
    }

    @FXML
    private void onEditCourseClick() {
        if (expandedCourseId == null) {
            showWarning("Selection requise", "Selectionnez un cours.");
            return;
        }

        try {
            Optional<Course> courseOptional = courseController.getCourseById(expandedCourseId);
            if (courseOptional.isEmpty()) {
                showWarning("Introuvable", "Le cours n'existe plus.");
                loadHierarchy();
                return;
            }

            Course current = courseOptional.get();
            Optional<String> title = promptText("Modifier cours", "Titre du cours", current.getTitle(), true);
            if (title.isEmpty()) {
                return;
            }
            Optional<String> code = promptText("Modifier cours", "Code du cours", defaultString(current.getCode()), false);
            if (code.isEmpty()) {
                return;
            }
            Optional<Float> totalHours = promptFloat(
                    "Modifier cours",
                    "Total heures (>= 0)",
                    String.valueOf(current.getTotalHours()),
                    0f
            );
            if (totalHours.isEmpty()) {
                return;
            }
            Optional<Integer> semester = promptOptionalInt(
                    "Modifier cours",
                    "Semestre (optionnel)",
                    current.getSemester() == null ? "" : String.valueOf(current.getSemester())
            );
            if (semester.isEmpty()) {
                return;
            }
            Optional<String> status = promptText(
                    "Modifier cours",
                    "Status",
                    normalizeOrDefault(current.getStatus(), "ACTIVE"),
                    false
            );
            if (status.isEmpty()) {
                return;
            }

            current.setTitle(title.get());
            current.setCode(normalizeOrNull(code.get()));
            current.setTotalHours(totalHours.get());
            current.setSemester(optionalIntOrNull(semester.get()));
            current.setStatus(normalizeOrDefault(status.get(), "ACTIVE"));
            courseController.updateCourse(current.getCourseId(), current);
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    @FXML
    private void onDeleteCourseClick() {
        if (expandedCourseId == null) {
            showWarning("Selection requise", "Selectionnez un cours.");
            return;
        }

        if (!showConfirmation(
                "Supprimer cours",
                "Supprimer le cours #" + expandedCourseId + " ?",
                "Les seances associees seront aussi supprimees."
        )) {
            return;
        }

        try {
            courseController.deleteCourse(expandedCourseId);
            expandedCourseId = null;
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    @FXML
    private void onAddSessionClick() {
        if (expandedClassroomId == null || expandedCourseId == null) {
            showWarning("Selection requise", "Selectionnez une classe et un cours.");
            return;
        }

        Optional<Session> session = promptSession(
                "Nouvelle seance",
                expandedClassroomId,
                expandedCourseId,
                null
        );
        if (session.isEmpty()) {
            return;
        }

        try {
            sessionController.createSession(session.get());
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Creation impossible", e.getMessage());
        }
    }

    private void onEditSessionClick(SessionItem item) {
        try {
            Optional<Session> sessionOptional = sessionController.getSessionById(item.id);
            if (sessionOptional.isEmpty()) {
                showWarning("Introuvable", "La seance n'existe plus.");
                loadHierarchy();
                return;
            }

            Session existing = sessionOptional.get();
            Integer classroomId = existing.getClassroomId() == null ? item.classroomId : existing.getClassroomId();
            Optional<Session> edited = promptSession("Modifier seance #" + item.id, classroomId, existing.getCourseId(), existing);
            if (edited.isEmpty()) {
                return;
            }

            sessionController.updateSession(item.id, edited.get());
            expandedClassroomId = classroomId;
            expandedCourseId = existing.getCourseId();
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    private void onDeleteSessionClick(SessionItem item) {
        if (!showConfirmation(
                "Supprimer seance",
                "Supprimer la seance #" + item.id + " ?",
                "Cette action est irreversible."
        )) {
            return;
        }

        try {
            sessionController.deleteSession(item.id);
            expandedClassroomId = item.classroomId;
            expandedCourseId = item.courseId;
            loadHierarchy();
        } catch (RuntimeException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    private void loadHierarchy() {
        classesContainer.getChildren().clear();
        summaryLabel.setText("Chargement...");

        Integer previousClassroomId = expandedClassroomId;
        Integer previousCourseId = expandedCourseId;

        try {
            cachedClassrooms = fetchHierarchy();

            if (previousClassroomId == null || !cachedClassrooms.containsKey(previousClassroomId)) {
                expandedClassroomId = null;
                expandedCourseId = null;
            } else {
                expandedClassroomId = previousClassroomId;
                ClassroomItem selected = cachedClassrooms.get(previousClassroomId);
                if (previousCourseId != null && selected != null && selected.courses.containsKey(previousCourseId)) {
                    expandedCourseId = previousCourseId;
                } else {
                    expandedCourseId = null;
                }
            }

            renderHierarchy(cachedClassrooms);
        } catch (SQLException e) {
            summaryLabel.setText("Erreur de chargement");
            classesContainer.getChildren().add(createMessage("Impossible de charger les donnees: " + e.getMessage(), "error-message"));
            updateActionState();
        }
    }

    private Map<Integer, ClassroomItem> fetchHierarchy() throws SQLException {
        String sql = "SELECT " +
                "c.classroom_id, c.name AS classroom_name, c.capacity, " +
                "co.course_id, co.code, co.title, " +
                "s.session_id, s.day_of_week, s.start_time, s.end_time, s.duration_hours, s.session_date, s.status " +
                "FROM classrooms c " +
                "LEFT JOIN sessions s ON s.classroom_id = c.classroom_id " +
                "LEFT JOIN courses co ON co.course_id = s.course_id " +
                "ORDER BY c.classroom_id, co.course_id, s.day_of_week, s.start_time";

        Map<Integer, ClassroomItem> classroomMap = new LinkedHashMap<>();

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int classroomId = resultSet.getInt("classroom_id");
                ClassroomItem classroom = classroomMap.computeIfAbsent(
                        classroomId,
                        id -> new ClassroomItem(
                                id,
                                safeString(resultSet, "classroom_name"),
                                safeInt(resultSet, "capacity")
                        )
                );

                Integer courseId = getNullableInt(resultSet, "course_id");
                if (courseId == null) {
                    continue;
                }

                CourseItem course = classroom.courses.computeIfAbsent(
                        courseId,
                        id -> new CourseItem(
                                id,
                                safeString(resultSet, "code"),
                                safeString(resultSet, "title")
                        )
                );

                Integer sessionId = getNullableInt(resultSet, "session_id");
                if (sessionId == null) {
                    continue;
                }

                course.sessions.add(new SessionItem(
                        sessionId,
                        classroom.id,
                        course.id,
                        safeString(resultSet, "day_of_week"),
                        resultSet.getTime("start_time"),
                        resultSet.getTime("end_time"),
                        resultSet.getDouble("duration_hours"),
                        resultSet.getDate("session_date"),
                        safeString(resultSet, "status")
                ));
            }
        }

        return classroomMap;
    }

    private void renderHierarchy(Map<Integer, ClassroomItem> classroomMap) {
        classesContainer.getChildren().clear();

        if (classroomMap.isEmpty()) {
            summaryLabel.setText("0 classes");
            classesContainer.getChildren().add(createMessage("Aucune classe trouvee.", "empty-message"));
            updateActionState();
            return;
        }

        if (expandedClassroomId == null) {
            summaryLabel.setText(classroomMap.size() + " classes");
        } else if (expandedCourseId == null) {
            summaryLabel.setText(classroomMap.size() + " classes | Classe #" + expandedClassroomId + " ouverte");
        } else {
            summaryLabel.setText(
                    classroomMap.size() + " classes | Classe #" + expandedClassroomId + " | Cours #" + expandedCourseId + " ouvert"
            );
        }

        for (ClassroomItem classroom : classroomMap.values()) {
            classesContainer.getChildren().add(buildClassCard(classroom));
        }

        updateActionState();
    }

    private VBox buildClassCard(ClassroomItem classroom) {
        VBox classCard = new VBox(10);
        classCard.getStyleClass().addAll("class-card", "clickable-card");

        boolean isExpanded = Objects.equals(expandedClassroomId, classroom.id);
        if (isExpanded) {
            classCard.getStyleClass().add("selected-card");
        }

        Label classTitle = new Label(classroom.name + " (ID #" + classroom.id + ")");
        classTitle.getStyleClass().add("class-title");

        Label classMeta = new Label("Capacite: " + classroom.capacity + " | Cours: " + classroom.courses.size());
        classMeta.getStyleClass().add("class-meta");

        Label classHint = new Label(isExpanded ? "Cliquez pour masquer les cours" : "Cliquez pour afficher les cours");
        classHint.getStyleClass().add("interaction-hint");

        classCard.getChildren().addAll(classTitle, classMeta, classHint);
        classCard.setOnMouseClicked(event -> onClassroomClick(classroom.id));

        if (!isExpanded) {
            return classCard;
        }
        if (classroom.courses.isEmpty()) {
            classCard.getChildren().add(createMessage("Aucun cours assigne a cette classe.", "empty-message"));
            return classCard;
        }

        for (CourseItem course : classroom.courses.values()) {
            classCard.getChildren().add(buildCourseCard(classroom.id, course));
        }

        return classCard;
    }

    private VBox buildCourseCard(int classroomId, CourseItem course) {
        VBox courseCard = new VBox(8);
        courseCard.getStyleClass().addAll("course-card", "clickable-card");

        boolean isExpanded = Objects.equals(expandedClassroomId, classroomId)
                && Objects.equals(expandedCourseId, course.id);
        if (isExpanded) {
            courseCard.getStyleClass().add("selected-card");
        }

        String courseLabel = (course.code == null || course.code.isBlank())
                ? course.title
                : course.code + " - " + course.title;

        Label courseTitle = new Label(courseLabel + " (ID #" + course.id + ")");
        courseTitle.getStyleClass().add("course-title");

        Label courseMeta = new Label("Sessions: " + course.sessions.size());
        courseMeta.getStyleClass().add("course-meta");

        Label courseHint = new Label(isExpanded ? "Cliquez pour masquer les seances" : "Cliquez pour afficher les seances");
        courseHint.getStyleClass().add("interaction-hint");

        courseCard.getChildren().addAll(courseTitle, courseMeta, courseHint);
        courseCard.setOnMouseClicked(event -> {
            event.consume();
            onCourseClick(classroomId, course.id);
        });

        if (!isExpanded) {
            return courseCard;
        }
        if (course.sessions.isEmpty()) {
            courseCard.getChildren().add(createMessage("Aucune session pour ce cours.", "empty-message"));
            return courseCard;
        }

        VBox sessionsContainer = new VBox(6);
        sessionsContainer.getStyleClass().add("sessions-container");
        for (SessionItem session : course.sessions) {
            sessionsContainer.getChildren().add(buildSessionRow(session));
        }
        courseCard.getChildren().add(sessionsContainer);

        return courseCard;
    }

    private HBox buildSessionRow(SessionItem session) {
        HBox row = new HBox(10);
        row.getStyleClass().add("session-row");

        Label slotLabel = new Label(formatSessionSlot(session));
        slotLabel.getStyleClass().add("session-slot");
        HBox.setHgrow(slotLabel, Priority.ALWAYS);

        Label detailsLabel = new Label(formatSessionDetails(session));
        detailsLabel.getStyleClass().add("session-details");

        Button editButton = new Button("Modifier");
        editButton.getStyleClass().addAll("tiny-action", "tiny-secondary");
        editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        editButton.setOnAction(event -> onEditSessionClick(session));

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("tiny-action", "tiny-danger");
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        deleteButton.setOnAction(event -> onDeleteSessionClick(session));

        row.getChildren().addAll(slotLabel, detailsLabel, editButton, deleteButton);
        return row;
    }

    private void onClassroomClick(int classroomId) {
        if (Objects.equals(expandedClassroomId, classroomId)) {
            expandedClassroomId = null;
            expandedCourseId = null;
        } else {
            expandedClassroomId = classroomId;
            expandedCourseId = null;
        }
        renderHierarchy(cachedClassrooms);
    }

    private void onCourseClick(int classroomId, int courseId) {
        if (!Objects.equals(expandedClassroomId, classroomId)) {
            expandedClassroomId = classroomId;
            expandedCourseId = null;
        }
        if (Objects.equals(expandedCourseId, courseId)) {
            expandedCourseId = null;
        } else {
            expandedCourseId = courseId;
        }
        renderHierarchy(cachedClassrooms);
    }

    private Optional<Session> promptSession(String title, Integer classroomId, int courseId, Session source) {
        Optional<String> day = promptDay(title, "Jour", source == null ? "MONDAY" : source.getDayOfWeek());
        if (day.isEmpty()) {
            return Optional.empty();
        }

        Optional<LocalTime> start = promptTime(title, "Heure debut (HH:mm)", source == null ? "08:00" : toHHmm(source.getStartTime()));
        if (start.isEmpty()) {
            return Optional.empty();
        }
        Optional<LocalTime> end = promptTime(title, "Heure fin (HH:mm)", source == null ? "09:30" : toHHmm(source.getEndTime()));
        if (end.isEmpty()) {
            return Optional.empty();
        }
        Optional<Float> duration = promptFloat(
                title,
                "Duree en heures (> 0)",
                source == null ? "1.5" : String.valueOf(source.getDurationHours()),
                0.0001f
        );
        if (duration.isEmpty()) {
            return Optional.empty();
        }
        Optional<LocalDate> date = promptOptionalDate(
                title,
                "Date (YYYY-MM-DD, optionnel)",
                source == null || source.getSessionDate() == null ? "" : source.getSessionDate().toString()
        );
        if (date.isEmpty()) {
            return Optional.empty();
        }
        Optional<Integer> teacher = promptOptionalInt(
                title,
                "Teacher ID (optionnel)",
                source == null || source.getTeacherId() == null ? "" : String.valueOf(source.getTeacherId())
        );
        if (teacher.isEmpty()) {
            return Optional.empty();
        }
        Optional<Boolean> recurring = promptBoolean(
                title,
                "Seance recurrente ?",
                source == null || source.isRecurring()
        );
        if (recurring.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> status = promptText(
                title,
                "Status",
                source == null ? "SCHEDULED" : normalizeOrDefault(source.getStatus(), "SCHEDULED"),
                false
        );
        if (status.isEmpty()) {
            return Optional.empty();
        }

        Session session = source == null ? new Session() : source;
        session.setClassroomId(classroomId);
        session.setCourseId(courseId);
        session.setDayOfWeek(day.get());
        session.setStartTime(Time.valueOf(start.get()));
        session.setEndTime(Time.valueOf(end.get()));
        session.setDurationHours(duration.get());
        LocalDate sessionDate = optionalDateOrNull(date.get());
        session.setSessionDate(sessionDate == null ? null : Date.valueOf(sessionDate));
        session.setTeacherId(optionalIntOrNull(teacher.get()));
        session.setRecurring(recurring.get());
        session.setStatus(normalizeOrDefault(status.get(), "SCHEDULED"));
        return Optional.of(session);
    }

    private Optional<String> promptDay(String title, String header, String defaultValue) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultValue, FXCollections.observableArrayList(WEEK_DAYS));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return dialog.showAndWait();
    }

    private Optional<Boolean> promptBoolean(String title, String header, boolean defaultValue) {
        String defaultLabel = defaultValue ? "Oui" : "Non";
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultLabel, FXCollections.observableArrayList("Oui", "Non"));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return dialog.showAndWait().map(choice -> "Oui".equals(choice));
    }

    private Optional<String> promptText(String title, String header, String defaultValue, boolean required) {
        String seed = defaultValue == null ? "" : defaultValue;
        while (true) {
            TextInputDialog dialog = new TextInputDialog(seed);
            dialog.setTitle(title);
            dialog.setHeaderText(header);
            Optional<String> value = dialog.showAndWait();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            String normalized = value.get().trim();
            if (!required || !normalized.isBlank()) {
                return Optional.of(normalized);
            }
            showWarning("Validation", "Ce champ est obligatoire.");
            seed = normalized;
        }
    }

    private Optional<Integer> promptInt(String title, String header, String defaultValue, int minValue) {
        String seed = defaultValue;
        while (true) {
            Optional<String> value = promptText(title, header, seed, true);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            try {
                int parsed = Integer.parseInt(value.get());
                if (parsed < minValue) {
                    showWarning("Validation", "La valeur doit etre >= " + minValue + ".");
                    seed = value.get();
                    continue;
                }
                return Optional.of(parsed);
            } catch (NumberFormatException e) {
                showWarning("Validation", "Entrez un entier valide.");
                seed = value.get();
            }
        }
    }

    private Optional<Float> promptFloat(String title, String header, String defaultValue, float minExclusive) {
        String seed = defaultValue;
        while (true) {
            Optional<String> value = promptText(title, header, seed, true);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            try {
                float parsed = Float.parseFloat(value.get());
                if (minExclusive == 0f && parsed < 0f) {
                    showWarning("Validation", "La valeur doit etre >= 0.");
                    seed = value.get();
                    continue;
                }
                if (minExclusive > 0f && parsed <= minExclusive) {
                    showWarning("Validation", "La valeur doit etre > 0.");
                    seed = value.get();
                    continue;
                }
                return Optional.of(parsed);
            } catch (NumberFormatException e) {
                showWarning("Validation", "Entrez un nombre valide.");
                seed = value.get();
            }
        }
    }

    private Optional<Integer> promptOptionalInt(String title, String header, String defaultValue) {
        String seed = defaultValue;
        while (true) {
            Optional<String> value = promptText(title, header, seed, false);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            if (value.get().isBlank()) {
                return Optional.of(Integer.MIN_VALUE);
            }
            try {
                return Optional.of(Integer.parseInt(value.get()));
            } catch (NumberFormatException e) {
                showWarning("Validation", "Entrez un entier valide ou laissez vide.");
                seed = value.get();
            }
        }
    }

    private Optional<LocalTime> promptTime(String title, String header, String defaultValue) {
        String seed = defaultValue;
        while (true) {
            Optional<String> value = promptText(title, header, seed, true);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of(LocalTime.parse(value.get()));
            } catch (DateTimeParseException e) {
                showWarning("Validation", "Format invalide. Utilisez HH:mm.");
                seed = value.get();
            }
        }
    }

    private Optional<LocalDate> promptOptionalDate(String title, String header, String defaultValue) {
        String seed = defaultValue;
        while (true) {
            Optional<String> value = promptText(title, header, seed, false);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            if (value.get().isBlank()) {
                return Optional.of(LocalDate.MIN);
            }
            try {
                return Optional.of(LocalDate.parse(value.get()));
            } catch (DateTimeParseException e) {
                showWarning("Validation", "Date invalide. Utilisez YYYY-MM-DD.");
                seed = value.get();
            }
        }
    }

    private void updateActionState() {
        boolean hasClassroomSelection = expandedClassroomId != null;
        boolean hasCourseSelection = hasClassroomSelection && expandedCourseId != null;

        if (editClassroomButton != null) {
            editClassroomButton.setDisable(!hasClassroomSelection);
        }
        if (deleteClassroomButton != null) {
            deleteClassroomButton.setDisable(!hasClassroomSelection);
        }
        if (editCourseButton != null) {
            editCourseButton.setDisable(!hasCourseSelection);
        }
        if (deleteCourseButton != null) {
            deleteCourseButton.setDisable(!hasCourseSelection);
        }
        if (addSessionButton != null) {
            addSessionButton.setDisable(!hasCourseSelection);
        }
        if (selectionLabel != null) {
            selectionLabel.setText(buildSelectionLabel());
        }
    }

    private String buildSelectionLabel() {
        if (expandedClassroomId == null) {
            return "Selection: aucune";
        }
        if (expandedCourseId == null) {
            return "Selection: classe #" + expandedClassroomId;
        }
        return "Selection: classe #" + expandedClassroomId + " | cours #" + expandedCourseId;
    }

    private Label createMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private String formatSessionSlot(SessionItem session) {
        String day = session.dayOfWeek == null ? "N/A" : session.dayOfWeek;
        String start = formatTime(session.startTime);
        String end = formatTime(session.endTime);
        if (session.sessionDate != null) {
            return day + " " + start + " - " + end + " | Date: " + session.sessionDate;
        }
        return day + " " + start + " - " + end;
    }

    private String formatSessionDetails(SessionItem session) {
        String duration = String.format(Locale.US, "%.1fh", session.durationHours);
        String status = (session.status == null || session.status.isBlank()) ? "N/A" : session.status;
        return "Duree: " + duration + " | Statut: " + status;
    }

    private String formatTime(Time time) {
        return time == null ? "N/A" : time.toLocalTime().toString();
    }

    private String toHHmm(Time time) {
        return time == null ? "08:00" : time.toLocalTime().toString().substring(0, 5);
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String safeString(ResultSet resultSet, String columnName) {
        try {
            return resultSet.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private int safeInt(ResultSet resultSet, String columnName) {
        try {
            return resultSet.getInt(columnName);
        } catch (SQLException e) {
            return 0;
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(defaultString(message));
        alert.showAndWait();
    }

    private void showWarning(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(defaultString(message));
        alert.showAndWait();
    }

    private boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeOrNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeOrDefault(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private Integer optionalIntOrNull(Integer value) {
        return value != null && value == Integer.MIN_VALUE ? null : value;
    }

    private LocalDate optionalDateOrNull(LocalDate value) {
        return value != null && value.equals(LocalDate.MIN) ? null : value;
    }

    private static final class ClassroomItem {
        private final int id;
        private final String name;
        private final int capacity;
        private final Map<Integer, CourseItem> courses = new LinkedHashMap<>();

        private ClassroomItem(int id, String name, int capacity) {
            this.id = id;
            this.name = (name == null || name.isBlank()) ? "Classe sans nom" : name;
            this.capacity = capacity;
        }
    }

    private static final class CourseItem {
        private final int id;
        private final String code;
        private final String title;
        private final List<SessionItem> sessions = new ArrayList<>();

        private CourseItem(int id, String code, String title) {
            this.id = id;
            this.code = code;
            this.title = (title == null || title.isBlank()) ? "Cours sans titre" : title;
        }
    }

    private static final class SessionItem {
        private final int id;
        private final int classroomId;
        private final int courseId;
        private final String dayOfWeek;
        private final Time startTime;
        private final Time endTime;
        private final double durationHours;
        private final Date sessionDate;
        private final String status;

        private SessionItem(
                int id,
                int classroomId,
                int courseId,
                String dayOfWeek,
                Time startTime,
                Time endTime,
                double durationHours,
                Date sessionDate,
                String status
        ) {
            this.id = id;
            this.classroomId = classroomId;
            this.courseId = courseId;
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationHours = durationHours;
            this.sessionDate = sessionDate;
            this.status = status;
        }
    }
}
