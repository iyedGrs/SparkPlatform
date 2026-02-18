package com.spark.platform.controllers;

import com.spark.platform.models.Classroom;
import com.spark.platform.models.Project;
import com.spark.platform.models.User;
import com.spark.platform.services.TeacherProjectService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Teacher Projects view.
 * Three drill-down levels:
 *   Level 1 – Classroom list (table)
 *   Level 2 – Projects in a classroom (table + create button)
 *   Level 3 – Project detail (summary + member list + assign/remove)
 *
 * Follows the same patterns as ProjectBoardController
 * (programmatic UI built on top of a minimal FXML shell).
 */
public class TeacherProjectsController {

    // ──── FXML bindings ────
    @FXML private Label viewTitle;
    @FXML private Label viewSubtitle;
    @FXML private HBox breadcrumbBar;
    @FXML private StackPane contentArea;

    // ──── Service ────
    private final TeacherProjectService service = new TeacherProjectService();

    // ──── Navigation state ────
    private Classroom selectedClassroom;
    private Project selectedProject;

    // Avatar color palette (matches ProjectBoardController)
    private static final String[] AVATAR_COLORS = {
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
        "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6366F1"
    };

    // ──── Init ────
    @FXML
    private void initialize() {
        showClassrooms();
    }

    // ═══════════════════════════════════════════════════════
    //   LEVEL 1 — Classroom List
    // ═══════════════════════════════════════════════════════

    private void showClassrooms() {
        selectedClassroom = null;
        selectedProject = null;

        viewTitle.setText("My Classes");
        viewSubtitle.setText("Select a class to manage its projects");
        renderBreadcrumb();

        contentArea.getChildren().clear();

        try {
            List<Classroom> classrooms = service.findAllClassrooms();

            VBox table = new VBox();
            table.getStyleClass().add("tp-table");
            table.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(table, Pos.TOP_LEFT);

            // ── Header row ──
            HBox header = buildTableHeader("Class Name", "Students", "Projects", "Status");
            table.getChildren().add(header);

            // ── Data rows ──
            for (Classroom c : classrooms) {
                int studentCount = service.countStudents(c.getClassroomId());
                int projectCount = service.countProjectsInClassroom(c.getClassroomId());
                HBox row = buildClassroomRow(c, studentCount, projectCount);
                table.getChildren().add(row);
            }

            if (classrooms.isEmpty()) {
                table.getChildren().add(buildEmptyState("No classes found"));
            }

            ScrollPane scroll = new ScrollPane(table);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("tp-scroll");
            contentArea.getChildren().add(scroll);

        } catch (SQLException e) {
            showError("Failed to load classrooms: " + e.getMessage());
        }
    }

    private HBox buildClassroomRow(Classroom c, int students, int projects) {
        HBox row = new HBox();
        row.getStyleClass().add("tp-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(56);
        row.setOnMouseClicked(e -> showProjects(c));

        // Name cell
        Label name = new Label(c.getName());
        name.getStyleClass().addAll("tp-cell", "tp-cell-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        // Student count
        Label studentsLabel = new Label(String.valueOf(students));
        studentsLabel.getStyleClass().addAll("tp-cell", "tp-cell-stat");
        studentsLabel.setPrefWidth(120);
        studentsLabel.setMinWidth(120);

        // Project count
        Label projectsLabel = new Label(String.valueOf(projects));
        projectsLabel.getStyleClass().addAll("tp-cell", "tp-cell-stat");
        projectsLabel.setPrefWidth(120);
        projectsLabel.setMinWidth(120);

        // Status badge
        Label statusBadge = new Label(c.getStatus());
        statusBadge.getStyleClass().addAll("tp-badge",
                "ACTIVE".equals(c.getStatus()) ? "tp-badge-active" : "tp-badge-default");
        HBox statusCell = new HBox(statusBadge);
        statusCell.getStyleClass().add("tp-cell");
        statusCell.setPrefWidth(120);
        statusCell.setMinWidth(120);
        statusCell.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(name, studentsLabel, projectsLabel, statusCell);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //   LEVEL 2 — Projects in a Classroom
    // ═══════════════════════════════════════════════════════

    private void showProjects(Classroom classroom) {
        selectedClassroom = classroom;
        selectedProject = null;

        viewTitle.setText(classroom.getName());
        viewSubtitle.setText("Projects in this class");
        renderBreadcrumb();

        contentArea.getChildren().clear();

        try {
            List<Project> projects = service.findProjectsByClassroom(classroom.getClassroomId());

            VBox container = new VBox(16);
            container.getStyleClass().add("tp-container");
            container.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(container, Pos.TOP_LEFT);

            // ── Toolbar ──
            HBox toolbar = new HBox(12);
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.getStyleClass().add("tp-toolbar");

            Label countLabel = new Label(projects.size() + " project" + (projects.size() != 1 ? "s" : ""));
            countLabel.getStyleClass().add("tp-count");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button createBtn = new Button("+ New Project");
            createBtn.getStyleClass().add("tp-create-btn");
            createBtn.setOnAction(e -> openCreateProjectDialog());

            toolbar.getChildren().addAll(countLabel, spacer, createBtn);
            container.getChildren().add(toolbar);

            // ── Table ──
            VBox table = new VBox();
            table.getStyleClass().add("tp-table");

            HBox header = buildTableHeader("Project", "Type", "Members", "Status", "Dates");
            table.getChildren().add(header);

            for (Project p : projects) {
                int memberCount = service.countMembers(p.getProjectId());
                HBox row = buildProjectRow(p, memberCount);
                table.getChildren().add(row);
            }

            if (projects.isEmpty()) {
                table.getChildren().add(buildEmptyState("No projects yet — create one!"));
            }

            container.getChildren().add(table);

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("tp-scroll");
            contentArea.getChildren().add(scroll);

        } catch (SQLException e) {
            showError("Failed to load projects: " + e.getMessage());
        }
    }

    private HBox buildProjectRow(Project p, int memberCount) {
        HBox row = new HBox();
        row.getStyleClass().add("tp-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(56);
        row.setOnMouseClicked(e -> showProjectDetail(p));

        // Title
        VBox titleBox = new VBox(2);
        Label title = new Label(p.getTitle());
        title.getStyleClass().add("tp-cell-name");
        Label desc = new Label(p.getDescription() != null ? truncate(p.getDescription(), 50) : "");
        desc.getStyleClass().add("tp-cell-muted");
        titleBox.getChildren().addAll(title, desc);
        titleBox.getStyleClass().add("tp-cell");
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.setMaxWidth(Double.MAX_VALUE);

        // Template type badge
        Label typeBadge = new Label(p.getTemplateType() != null ? p.getTemplateType() : "STANDARD");
        typeBadge.getStyleClass().addAll("tp-badge", "tp-badge-type");
        HBox typeCell = new HBox(typeBadge);
        typeCell.getStyleClass().add("tp-cell");
        typeCell.setPrefWidth(110);
        typeCell.setMinWidth(110);
        typeCell.setAlignment(Pos.CENTER_LEFT);

        // Members
        Label membersLabel = new Label(memberCount + " member" + (memberCount != 1 ? "s" : ""));
        membersLabel.getStyleClass().addAll("tp-cell", "tp-cell-stat");
        membersLabel.setPrefWidth(110);
        membersLabel.setMinWidth(110);

        // Status
        Label statusBadge = new Label(p.getStatus());
        statusBadge.getStyleClass().addAll("tp-badge",
                "ACTIVE".equals(p.getStatus()) ? "tp-badge-active" : "tp-badge-default");
        HBox statusCell = new HBox(statusBadge);
        statusCell.getStyleClass().add("tp-cell");
        statusCell.setPrefWidth(100);
        statusCell.setMinWidth(100);
        statusCell.setAlignment(Pos.CENTER_LEFT);

        // Dates
        String dateStr = "";
        if (p.getStartDate() != null && p.getEndDate() != null) {
            dateStr = p.getStartDate().toString() + " → " + p.getEndDate().toString();
        }
        Label dates = new Label(dateStr);
        dates.getStyleClass().addAll("tp-cell", "tp-cell-muted");
        dates.setPrefWidth(180);
        dates.setMinWidth(180);

        row.getChildren().addAll(titleBox, typeCell, membersLabel, statusCell, dates);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //   LEVEL 3 — Project Detail (Summary + Members)
    // ═══════════════════════════════════════════════════════

    private void showProjectDetail(Project project) {
        selectedProject = project;

        viewTitle.setText(project.getTitle());
        viewSubtitle.setText(project.getDescription() != null ? project.getDescription() : "");
        renderBreadcrumb();

        contentArea.getChildren().clear();

        try {
            List<Map<String, Object>> members = service.findProjectMembers(project.getProjectId());
            Map<String, Integer> taskStats = service.getTaskStats(project.getProjectId());

            VBox container = new VBox(24);
            container.getStyleClass().add("tp-container");
            StackPane.setAlignment(container, Pos.TOP_LEFT);

            // ── Summary cards row ──
            HBox summaryRow = new HBox(16);
            summaryRow.getStyleClass().add("tp-summary-row");

            summaryRow.getChildren().addAll(
                buildSummaryCard("Members", String.valueOf(members.size()), "#3B82F6"),
                buildSummaryCard("Total Tasks", String.valueOf(taskStats.getOrDefault("total", 0)), "#8B5CF6"),
                buildSummaryCard("Completed", String.valueOf(taskStats.getOrDefault("done", 0)), "#10B981"),
                buildSummaryCard("In Progress", String.valueOf(taskStats.getOrDefault("inProgress", 0)), "#F59E0B")
            );

            container.getChildren().add(summaryRow);

            // ── Project info ──
            VBox infoSection = new VBox(8);
            infoSection.getStyleClass().add("tp-info-section");

            Label infoTitle = new Label("Project Details");
            infoTitle.getStyleClass().add("tp-section-title");
            infoSection.getChildren().add(infoTitle);

            HBox infoGrid = new HBox(32);
            infoGrid.getStyleClass().add("tp-info-grid");
            infoGrid.getChildren().addAll(
                buildInfoItem("Template", project.getTemplateType() != null ? project.getTemplateType() : "STANDARD"),
                buildInfoItem("Status", project.getStatus()),
                buildInfoItem("Start", project.getStartDate() != null ? project.getStartDate().toString() : "—"),
                buildInfoItem("End", project.getEndDate() != null ? project.getEndDate().toString() : "—"),
                buildInfoItem("Repository", project.getRepoUrl() != null ? project.getRepoUrl() : "—")
            );
            infoSection.getChildren().add(infoGrid);
            container.getChildren().add(infoSection);

            // ── Members section ──
            VBox membersSection = new VBox(12);
            membersSection.getStyleClass().add("tp-members-section");

            HBox membersHeader = new HBox(12);
            membersHeader.setAlignment(Pos.CENTER_LEFT);
            Label membersTitle = new Label("Team Members (" + members.size() + ")");
            membersTitle.getStyleClass().add("tp-section-title");
            Region mSpacer = new Region();
            HBox.setHgrow(mSpacer, Priority.ALWAYS);
            Button addMemberBtn = new Button("+ Add Student");
            addMemberBtn.getStyleClass().add("tp-create-btn");
            addMemberBtn.setOnAction(e -> openAddMemberDialog(project));
            membersHeader.getChildren().addAll(membersTitle, mSpacer, addMemberBtn);
            membersSection.getChildren().add(membersHeader);

            // Members table
            VBox membersTable = new VBox();
            membersTable.getStyleClass().add("tp-table");

            HBox mHeader = buildTableHeader("Name", "Role", "Email", "Actions");
            membersTable.getChildren().add(mHeader);

            int colorIdx = 0;
            for (Map<String, Object> member : members) {
                HBox mRow = buildMemberRow(member, project, colorIdx);
                membersTable.getChildren().add(mRow);
                colorIdx++;
            }

            if (members.isEmpty()) {
                membersTable.getChildren().add(buildEmptyState("No members assigned yet"));
            }

            membersSection.getChildren().add(membersTable);
            container.getChildren().add(membersSection);

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("tp-scroll");
            contentArea.getChildren().add(scroll);

        } catch (SQLException e) {
            showError("Failed to load project detail: " + e.getMessage());
        }
    }

    private HBox buildMemberRow(Map<String, Object> member, Project project, int colorIdx) {
        HBox row = new HBox();
        row.getStyleClass().add("tp-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(56);

        int userId = (int) member.get("userId");
        String name = (String) member.get("name");
        String email = (String) member.get("email");
        String role = (String) member.get("role");

        // Avatar + name
        HBox nameCell = new HBox(10);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.getStyleClass().add("tp-cell");
        HBox.setHgrow(nameCell, Priority.ALWAYS);
        nameCell.setMaxWidth(Double.MAX_VALUE);

        StackPane avatar = buildAvatar(name, 32, AVATAR_COLORS[colorIdx % AVATAR_COLORS.length]);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("tp-cell-name");
        nameCell.getChildren().addAll(avatar, nameLabel);

        // Role badge
        Label roleBadge = new Label(role != null ? role : "MEMBER");
        roleBadge.getStyleClass().addAll("tp-badge", "tp-badge-role");
        HBox roleCell = new HBox(roleBadge);
        roleCell.getStyleClass().add("tp-cell");
        roleCell.setPrefWidth(140);
        roleCell.setMinWidth(140);
        roleCell.setAlignment(Pos.CENTER_LEFT);

        // Email
        Label emailLabel = new Label(email != null ? email : "");
        emailLabel.getStyleClass().addAll("tp-cell", "tp-cell-muted");
        HBox.setHgrow(emailLabel, Priority.ALWAYS);
        emailLabel.setMaxWidth(Double.MAX_VALUE);

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("tp-remove-btn");
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove " + name + " from this project?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm Removal");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        service.removeMember(project.getProjectId(), userId);
                        showProjectDetail(project); // refresh
                    } catch (SQLException ex) {
                        showError("Failed to remove member: " + ex.getMessage());
                    }
                }
            });
        });
        HBox actionCell = new HBox(removeBtn);
        actionCell.getStyleClass().add("tp-cell");
        actionCell.setPrefWidth(100);
        actionCell.setMinWidth(100);
        actionCell.setAlignment(Pos.CENTER);

        row.getChildren().addAll(nameCell, roleCell, emailLabel, actionCell);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //   DIALOGS
    // ═══════════════════════════════════════════════════════

    /** Open dialog to create a new project in the current classroom. */
    private void openCreateProjectDialog() {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Create Project");
        dialog.setHeaderText("New project for " + selectedClassroom.getName());

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/teacher-projects.css").toExternalForm());
        dp.getStyleClass().add("tp-dialog");
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        TextField titleField = new TextField();
        titleField.setPromptText("Project title");
        titleField.getStyleClass().add("tp-input");

        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        descField.getStyleClass().add("tp-input");

        TextField repoField = new TextField();
        repoField.setPromptText("https://github.com/...");
        repoField.getStyleClass().add("tp-input");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(
            "PI_DEV", "PI_IOT", "STANDARD", "CUSTOM"
        ));
        typeCombo.getSelectionModel().select("STANDARD");
        typeCombo.getStyleClass().add("tp-input");

        DatePicker startPicker = new DatePicker(LocalDate.now());
        startPicker.getStyleClass().add("tp-input");

        DatePicker endPicker = new DatePicker(LocalDate.now().plusMonths(3));
        endPicker.getStyleClass().add("tp-input");

        grid.add(new Label("Title *"), 0, 0);      grid.add(titleField, 1, 0);
        grid.add(new Label("Description"), 0, 1);   grid.add(descField, 1, 1);
        grid.add(new Label("Repository"), 0, 2);    grid.add(repoField, 1, 2);
        grid.add(new Label("Template"), 0, 3);       grid.add(typeCombo, 1, 3);
        grid.add(new Label("Start Date"), 0, 4);     grid.add(startPicker, 1, 4);
        grid.add(new Label("End Date"), 0, 5);        grid.add(endPicker, 1, 5);

        // Style grid labels
        for (var node : grid.getChildren()) {
            if (node instanceof Label l && GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == 0) {
                l.getStyleClass().add("tp-form-label");
            }
        }

        dp.setContent(grid);

        // Validation
        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        okBtn.setText("Create");
        okBtn.setDisable(true);
        titleField.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        // Error label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("tp-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        grid.add(errorLabel, 0, 6, 2, 1);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Project p = new Project();
                p.setTitle(titleField.getText().trim());
                p.setDescription(descField.getText().trim().isEmpty() ? null : descField.getText().trim());
                p.setRepoUrl(repoField.getText().trim().isEmpty() ? null : repoField.getText().trim());
                p.setTemplateType(typeCombo.getValue());
                if (startPicker.getValue() != null) {
                    p.setStartDate(java.sql.Date.valueOf(startPicker.getValue()));
                }
                if (endPicker.getValue() != null) {
                    p.setEndDate(java.sql.Date.valueOf(endPicker.getValue()));
                }
                p.setStatus("ACTIVE");
                p.setClassroomId(selectedClassroom.getClassroomId());
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(project -> {
            try {
                service.createProject(project);
                showProjects(selectedClassroom); // refresh
            } catch (SQLException ex) {
                showError("Failed to create project: " + ex.getMessage());
            }
        });
    }

    /** Open dialog to add a student to the current project. */
    private void openAddMemberDialog(Project project) {
        try {
            List<User> available = service.findAvailableStudents(
                    selectedClassroom.getClassroomId(), project.getProjectId());

            if (available.isEmpty()) {
                showInfo("All students in " + selectedClassroom.getName() + " are already assigned to this project.");
                return;
            }

            Dialog<int[]> dialog = new Dialog<>();
            dialog.setTitle("Add Student");
            dialog.setHeaderText("Assign a student to " + project.getTitle());

            DialogPane dp = dialog.getDialogPane();
            dp.getStylesheets().add(getClass().getResource("/css/teacher-projects.css").toExternalForm());
            dp.getStyleClass().add("tp-dialog");
            dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(12);
            grid.setPadding(new Insets(16));

            ComboBox<User> studentCombo = new ComboBox<>(FXCollections.observableArrayList(available));
            studentCombo.setPromptText("Select student...");
            studentCombo.getStyleClass().add("tp-input");
            studentCombo.setConverter(new javafx.util.StringConverter<User>() {
                @Override public String toString(User u) { return u == null ? "" : u.getName() + " (" + u.getEmail() + ")"; }
                @Override public User fromString(String s) { return null; }
            });

            ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(
                    "MEMBER", "DEVELOPER", "TESTER", "SCRUM_MASTER", "PO"));
            roleCombo.getSelectionModel().select("MEMBER");
            roleCombo.getStyleClass().add("tp-input");

            Label studentLabel = new Label("Student *");
            studentLabel.getStyleClass().add("tp-form-label");
            Label roleLabel = new Label("Role");
            roleLabel.getStyleClass().add("tp-form-label");

            grid.add(studentLabel, 0, 0);   grid.add(studentCombo, 1, 0);
            grid.add(roleLabel, 0, 1);       grid.add(roleCombo, 1, 1);

            dp.setContent(grid);

            Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
            okBtn.setText("Add");
            okBtn.setDisable(true);
            studentCombo.valueProperty().addListener((obs, o, n) -> okBtn.setDisable(n == null));

            dialog.setResultConverter(bt -> {
                if (bt == ButtonType.OK && studentCombo.getValue() != null) {
                    return new int[]{studentCombo.getValue().getUserId()};
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                try {
                    service.addMember(project.getProjectId(), result[0], roleCombo.getValue());
                    showProjectDetail(project); // refresh
                } catch (SQLException ex) {
                    showError("Failed to add member: " + ex.getMessage());
                }
            });

        } catch (SQLException e) {
            showError("Failed to load available students: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //   BREADCRUMB NAV
    // ═══════════════════════════════════════════════════════

    private void renderBreadcrumb() {
        breadcrumbBar.getChildren().clear();

        // Root: "Classes"
        Hyperlink classesLink = new Hyperlink("Classes");
        classesLink.getStyleClass().add("tp-breadcrumb-link");
        classesLink.setOnAction(e -> showClassrooms());
        breadcrumbBar.getChildren().add(classesLink);

        if (selectedClassroom != null) {
            breadcrumbBar.getChildren().add(buildBreadcrumbSep());
            Hyperlink classLink = new Hyperlink(selectedClassroom.getName());
            classLink.getStyleClass().add("tp-breadcrumb-link");
            classLink.setOnAction(e -> showProjects(selectedClassroom));
            breadcrumbBar.getChildren().add(classLink);
        }

        if (selectedProject != null) {
            breadcrumbBar.getChildren().add(buildBreadcrumbSep());
            Label projLabel = new Label(selectedProject.getTitle());
            projLabel.getStyleClass().add("tp-breadcrumb-current");
            breadcrumbBar.getChildren().add(projLabel);
        }
    }

    private Label buildBreadcrumbSep() {
        Label sep = new Label("›");
        sep.getStyleClass().add("tp-breadcrumb-sep");
        return sep;
    }

    // ═══════════════════════════════════════════════════════
    //   UI HELPERS
    // ═══════════════════════════════════════════════════════

    private HBox buildTableHeader(String... columns) {
        HBox header = new HBox();
        header.getStyleClass().add("tp-header-row");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(40);

        for (int i = 0; i < columns.length; i++) {
            Label col = new Label(columns[i]);
            col.getStyleClass().add("tp-header-cell");
            if (i == 0) {
                HBox.setHgrow(col, Priority.ALWAYS);
                col.setMaxWidth(Double.MAX_VALUE);
            } else {
                col.setPrefWidth(i == columns.length - 1 && columns.length == 5 ? 180 : 120);
                col.setMinWidth(i == columns.length - 1 && columns.length == 5 ? 180 : 120);
                // Special widths for project table
                if (columns.length == 5) {
                    if (i == 1) { col.setPrefWidth(110); col.setMinWidth(110); }
                    else if (i == 2) { col.setPrefWidth(110); col.setMinWidth(110); }
                    else if (i == 3) { col.setPrefWidth(100); col.setMinWidth(100); }
                }
                // Special widths for members table
                if (columns.length == 4) {
                    if (i == 1) { col.setPrefWidth(140); col.setMinWidth(140); }
                    else if (i == 3) { col.setPrefWidth(100); col.setMinWidth(100); }
                }
            }
        }

        header.getChildren().addAll(header.getChildren().isEmpty() ?
                java.util.Collections.emptyList() : java.util.Collections.emptyList());

        // Re-build properly
        header.getChildren().clear();
        for (int i = 0; i < columns.length; i++) {
            Label col = new Label(columns[i]);
            col.getStyleClass().add("tp-header-cell");
            if (i == 0) {
                HBox.setHgrow(col, Priority.ALWAYS);
                col.setMaxWidth(Double.MAX_VALUE);
            } else {
                int w = 120; // default
                if (columns.length == 5) {
                    if (i == 1) w = 110;
                    else if (i == 2) w = 110;
                    else if (i == 3) w = 100;
                    else if (i == 4) w = 180;
                }
                if (columns.length == 4) {
                    if (i == 1) w = 140;
                    else if (i == 3) w = 100;
                }
                col.setPrefWidth(w);
                col.setMinWidth(w);
            }
            header.getChildren().add(col);
        }

        return header;
    }

    private VBox buildSummaryCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("tp-summary-card");
        card.setPrefWidth(160);
        card.setMinWidth(140);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("tp-summary-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("tp-summary-label");

        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }

    private VBox buildInfoItem(String label, String value) {
        VBox item = new VBox(2);
        Label l = new Label(label);
        l.getStyleClass().add("tp-info-label");
        Label v = new Label(value);
        v.getStyleClass().add("tp-info-value");
        item.getChildren().addAll(l, v);
        return item;
    }

    private StackPane buildAvatar(String name, double size, String bgColor) {
        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split("\\s+");
            initials = String.valueOf(parts[0].charAt(0));
            if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
        }
        initials = initials.toUpperCase();

        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: " + (size * 0.4) + "px; -fx-font-weight: 600;");

        StackPane circle = new StackPane(initialsLabel);
        circle.setPrefSize(size, size);
        circle.setMinSize(size, size);
        circle.setMaxSize(size, size);
        circle.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: " + (size / 2) + ";");
        circle.setAlignment(Pos.CENTER);

        return circle;
    }

    private Label buildEmptyState(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("tp-empty-state");
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        empty.setPrefHeight(80);
        return empty;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }

    // ──── Alerts ────
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText("Information");
        alert.showAndWait();
    }
}
