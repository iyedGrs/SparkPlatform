package com.spark.platform.controllers;

import com.spark.platform.models.Notification;
import com.spark.platform.models.User;
import com.spark.platform.services.NotificationService;
import com.spark.platform.services.UserService;
import com.spark.platform.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing users (CRUD).
 * Follows the same programmatic UI pattern as TeacherProjectsController.
 */
public class UserManagementController {

    @FXML private Label viewTitle;
    @FXML private Label viewSubtitle;
    @FXML private StackPane contentArea;

    private final UserService userService = new UserService();
    private final NotificationService notificationService = new NotificationService();

    private String currentFilter = "ALL";
    private List<User> allUsers;
    private Map<Integer, String> classroomMap = new HashMap<>();

    private static final String[] AVATAR_COLORS = {
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
            "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6366F1"
    };

    @FXML
    private void initialize() {
        loadClassrooms();
        loadUsers();
    }

    private void loadClassrooms() {
        try {
            List<Object[]> classrooms = userService.findAllClassrooms();
            classroomMap.clear();
            for (Object[] row : classrooms) {
                classroomMap.put((Integer) row[0], (String) row[1]);
            }
        } catch (SQLException e) {
            System.err.println("[UserMgmt] Failed to load classrooms: " + e.getMessage());
        }
    }

    private void loadUsers() {
        contentArea.getChildren().clear();
        try {
            allUsers = userService.findAll();
            List<User> filtered = filterUsers(allUsers, currentFilter);

            VBox container = new VBox(16);
            container.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(container, Pos.TOP_LEFT);

            container.getChildren().add(buildSummaryRow());
            container.getChildren().add(buildToolbar(filtered.size()));
            container.getChildren().add(buildTable(filtered));

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("um-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentArea.getChildren().add(scroll);

        } catch (SQLException e) {
            Label error = new Label("Failed to load users: " + e.getMessage());
            error.getStyleClass().add("um-empty-state");
            contentArea.getChildren().add(error);
        }
    }

    private List<User> filterUsers(List<User> users, String filter) {
        if ("ALL".equals(filter)) return users;
        return users.stream()
                .filter(u -> filter.equals(u.getUserType()))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════
    // SUMMARY CARDS — Enhanced with Gradients, Icons & Animations
    // ═══════════════════════════════════════════════════════

    private HBox buildSummaryRow() {
        long students = allUsers.stream().filter(u -> "STUDENT".equals(u.getUserType())).count();
        long teachers = allUsers.stream().filter(u -> "TEACHER".equals(u.getUserType())).count();
        long admins = allUsers.stream().filter(u -> "ADMINISTRATOR".equals(u.getUserType())).count();

        HBox row = new HBox(20);
        row.getChildren().addAll(
                buildSummaryCard("Total Users", String.valueOf(allUsers.size()), "um-card-total", "um-icon-users"),
                buildSummaryCard("Students", String.valueOf(students), "um-card-students", "um-icon-student"),
                buildSummaryCard("Teachers", String.valueOf(teachers), "um-card-teachers", "um-icon-teacher"),
                buildSummaryCard("Admins", String.valueOf(admins), "um-card-admins", "um-icon-admin")
        );
        return row;
    }

    private StackPane buildSummaryCard(String label, String value, String cardStyleClass, String iconStyleClass) {
        // Outer wrapper (StackPane for layered effects)
        StackPane cardWrapper = new StackPane();
        cardWrapper.getStyleClass().addAll("um-summary-card", cardStyleClass);
        HBox.setHgrow(cardWrapper, Priority.ALWAYS);
        
        // Add hover animation
        cardWrapper.setOnMouseEntered(e -> {
            cardWrapper.setScaleX(1.03);
            cardWrapper.setScaleY(1.03);
        });
        cardWrapper.setOnMouseExited(e -> {
            cardWrapper.setScaleX(1.0);
            cardWrapper.setScaleY(1.0);
        });

        // Decorative orbs (background)
        Region orb1 = new Region();
        orb1.getStyleClass().addAll("um-card-orb", "um-card-orb-1");
        StackPane.setAlignment(orb1, Pos.TOP_RIGHT);
        orb1.setTranslateX(20);
        orb1.setTranslateY(-20);

        Region orb2 = new Region();
        orb2.getStyleClass().addAll("um-card-orb", "um-card-orb-2");
        StackPane.setAlignment(orb2, Pos.BOTTOM_LEFT);
        orb2.setTranslateX(-10);
        orb2.setTranslateY(10);

        // Main content container
        HBox contentRow = new HBox(16);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(contentRow, Pos.CENTER_LEFT);

        // Icon circle
        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("um-card-icon-circle");

        Region icon = new Region();
        icon.getStyleClass().addAll("um-card-icon", iconStyleClass);
        iconCircle.getChildren().add(icon);

        // Text content
        VBox textContent = new VBox(2);
        textContent.setAlignment(Pos.CENTER_LEFT);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("um-summary-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("um-summary-label");

        textContent.getChildren().addAll(valueLabel, nameLabel);

        contentRow.getChildren().addAll(iconCircle, textContent);

        // Assemble layers
        cardWrapper.getChildren().addAll(orb1, orb2, contentRow);

        return cardWrapper;
    }

    // ═══════════════════════════════════════════════════════
    // TOOLBAR
    // ═══════════════════════════════════════════════════════

    private HBox buildToolbar(int filteredCount) {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox filters = new HBox(8);
        String[][] filterDefs = {
                {"ALL", "All"},
                {"STUDENT", "Students"},
                {"TEACHER", "Teachers"},
                {"ADMINISTRATOR", "Admins"}
        };
        for (String[] def : filterDefs) {
            Button btn = new Button(def[1]);
            btn.getStyleClass().add("um-filter-btn");
            if (def[0].equals(currentFilter)) {
                btn.getStyleClass().add("active");
            }
            String filterVal = def[0];
            btn.setOnAction(e -> {
                currentFilter = filterVal;
                loadUsers();
            });
            filters.getChildren().add(btn);
        }

        Label count = new Label(filteredCount + " user" + (filteredCount != 1 ? "s" : ""));
        count.getStyleClass().add("um-count");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button createBtn = new Button("+ Create User");
        createBtn.getStyleClass().add("um-create-btn");
        createBtn.setOnAction(e -> openCreateUserDialog());

        toolbar.getChildren().addAll(filters, count, spacer, createBtn);
        return toolbar;
    }

    // ═══════════════════════════════════════════════════════
    // TABLE
    // ═══════════════════════════════════════════════════════

    private VBox buildTable(List<User> users) {
        VBox table = new VBox();
        table.getStyleClass().add("um-table-container");

        // Header row
        HBox header = new HBox();
        header.getStyleClass().add("um-header-row");
        header.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(
                headerCell("Name", 200),
                headerCell("Email", 200),
                headerCell("Role", 100),
                headerCell("Classroom", 100),
                headerCell("Status", 80),
                headerCell("Actions", 180)
        );
        table.getChildren().add(header);

        if (users.isEmpty()) {
            Label empty = new Label("No users found");
            empty.getStyleClass().add("um-empty-state");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            table.getChildren().add(empty);
        } else {
            for (User user : users) {
                table.getChildren().add(buildUserRow(user));
            }
        }

        return table;
    }

    private Label headerCell(String text, double minWidth) {
        Label label = new Label(text);
        label.getStyleClass().add("um-header-cell");
        label.setMinWidth(minWidth);
        label.setPrefWidth(minWidth);
        HBox.setHgrow(label, Priority.SOMETIMES);
        return label;
    }

    private HBox buildUserRow(User user) {
        HBox row = new HBox();
        row.getStyleClass().add("um-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar + Name
        HBox nameCell = new HBox(10);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.setMinWidth(200);
        nameCell.setPrefWidth(200);
        HBox.setHgrow(nameCell, Priority.SOMETIMES);

        StackPane avatar = buildAvatar(user.getName(), user.getUserId());
        Label nameLabel = new Label(user.getName());
        nameLabel.getStyleClass().add("um-cell-name");
        nameCell.getChildren().addAll(avatar, nameLabel);

        // Email
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("um-cell-email");
        emailLabel.setMinWidth(200);
        emailLabel.setPrefWidth(200);
        HBox.setHgrow(emailLabel, Priority.SOMETIMES);

        // Role badge
        Label roleBadge = new Label(formatRole(user.getUserType()));
        roleBadge.getStyleClass().addAll("um-badge", getRoleBadgeClass(user.getUserType()));
        HBox roleCell = new HBox(roleBadge);
        roleCell.setAlignment(Pos.CENTER_LEFT);
        roleCell.setMinWidth(100);
        roleCell.setPrefWidth(100);
        HBox.setHgrow(roleCell, Priority.SOMETIMES);

        // Classroom
        String classroomName = user.getClassroomId() != null
                ? classroomMap.getOrDefault(user.getClassroomId(), "--")
                : "--";
        Label classroomLabel = new Label(classroomName);
        classroomLabel.getStyleClass().add("um-cell-muted");
        classroomLabel.setMinWidth(100);
        classroomLabel.setPrefWidth(100);
        HBox.setHgrow(classroomLabel, Priority.SOMETIMES);

        // Status badge
        Label statusBadge = new Label(user.getStatus() != null ? user.getStatus() : "ACTIVE");
        String statusClass = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "um-badge-active" : "um-badge-inactive";
        statusBadge.getStyleClass().addAll("um-badge", statusClass);
        HBox statusCell = new HBox(statusBadge);
        statusCell.setAlignment(Pos.CENTER_LEFT);
        statusCell.setMinWidth(80);
        statusCell.setPrefWidth(80);
        HBox.setHgrow(statusCell, Priority.SOMETIMES);

        // Actions
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMinWidth(180);
        actions.setPrefWidth(180);
        HBox.setHgrow(actions, Priority.SOMETIMES);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("um-edit-btn");
        editBtn.setOnAction(e -> openEditUserDialog(user));

        Button resetPwdBtn = new Button("Reset Pwd");
        resetPwdBtn.getStyleClass().add("um-reset-btn");
        resetPwdBtn.setOnAction(e -> handleResetPassword(user));

        // Don't allow deleting the current user
        int currentUserId = SessionManager.getInstance().getUserId();
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("um-delete-btn");
        deleteBtn.setOnAction(e -> handleDeleteUser(user));
        if (user.getUserId() == currentUserId) {
            deleteBtn.setDisable(true);
            deleteBtn.setOpacity(0.4);
        }

        actions.getChildren().addAll(editBtn, resetPwdBtn, deleteBtn);

        row.getChildren().addAll(nameCell, emailLabel, roleCell, classroomLabel, statusCell, actions);
        return row;
    }

    private StackPane buildAvatar(String name, int userId) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("um-avatar");
        avatar.setMinSize(32, 32);
        avatar.setPrefSize(32, 32);
        avatar.setMaxSize(32, 32);

        String color = AVATAR_COLORS[Math.abs(userId) % AVATAR_COLORS.length];
        avatar.setStyle("-fx-background-color: " + color + ";");

        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split("\\s+");
            initials = String.valueOf(parts[0].charAt(0));
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label initialsLabel = new Label(initials.toUpperCase());
        initialsLabel.getStyleClass().add("um-avatar-text");
        avatar.getChildren().add(initialsLabel);

        return avatar;
    }

    // ═══════════════════════════════════════════════════════
    // DIALOG HELPERS
    // ═══════════════════════════════════════════════════════

    private static final String DIALOG_CSS =
            UserManagementController.class.getResource("/css/user-management.css").toExternalForm();

    private void applyDialogStyle(DialogPane pane, String styleClass) {
        pane.getStylesheets().add(DIALOG_CSS);
        pane.getStyleClass().add(styleClass);
        pane.setHeaderText(null);
        pane.setGraphic(null);
    }

    private VBox makeFieldGroup(String labelText, javafx.scene.Node input) {
        VBox group = new VBox();
        group.getStyleClass().add("um-field-group");
        Label label = new Label(labelText);
        label.getStyleClass().add("um-field-label");
        group.getChildren().addAll(label, input);
        return group;
    }

    // ═══════════════════════════════════════════════════════
    // CREATE USER DIALOG — Enhanced with Gradient Header
    // ═══════════════════════════════════════════════════════

    private void openCreateUserDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Spark");

        ButtonType createBtnType = new ButtonType("Create Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);
        applyDialogStyle(dialog.getDialogPane(), "um-dialog-pane");

        // ── Build content ──
        VBox content = new VBox();

        // Enhanced Gradient Header with icon
        StackPane headerWrapper = new StackPane();
        headerWrapper.getStyleClass().add("um-dialog-header");
        
        // Decorative orbs in header
        Region headerOrb1 = new Region();
        headerOrb1.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-1");
        StackPane.setAlignment(headerOrb1, Pos.TOP_RIGHT);
        headerOrb1.setTranslateX(30);
        headerOrb1.setTranslateY(-30);
        headerOrb1.setMouseTransparent(true);
        
        Region headerOrb2 = new Region();
        headerOrb2.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-2");
        StackPane.setAlignment(headerOrb2, Pos.BOTTOM_LEFT);
        headerOrb2.setTranslateX(-20);
        headerOrb2.setTranslateY(15);
        headerOrb2.setMouseTransparent(true);
        
        // Header content
        VBox headerContent = new VBox(6);
        headerContent.setAlignment(Pos.CENTER_LEFT);
        
        // Icon container
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("um-dialog-header-icon");
        Region headerIcon = new Region();
        headerIcon.getStyleClass().add("um-dialog-header-icon-graphic");
        headerIcon.setStyle("-fx-shape: 'M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z';");
        iconContainer.getChildren().add(headerIcon);
        
        Label title = new Label("Create New User");
        title.getStyleClass().add("um-dialog-title");
        Label subtitle = new Label("Add a new student, teacher, or administrator account");
        subtitle.getStyleClass().add("um-dialog-subtitle");
        headerContent.getChildren().addAll(iconContainer, title, subtitle);
        
        headerWrapper.getChildren().addAll(headerOrb1, headerOrb2, headerContent);

        // Form fields
        VBox form = new VBox();
        form.getStyleClass().add("um-dialog-form");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Ahmed Ben Ali");
        nameField.getStyleClass().add("um-field-input");

        TextField emailField = new TextField();
        emailField.setPromptText("e.g. ahmed.benali@spark.tn");
        emailField.getStyleClass().add("um-field-input");

        // Role + Classroom in a row
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "STUDENT", "TEACHER", "ADMINISTRATOR"
        ));
        roleCombo.getStyleClass().add("um-field-combo");
        roleCombo.setValue("STUDENT");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(roleCombo, Priority.ALWAYS);

        ComboBox<String> classroomCombo = new ComboBox<>();
        classroomCombo.getStyleClass().add("um-field-combo");
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setPromptText("Select classroom");
        HBox.setHgrow(classroomCombo, Priority.ALWAYS);

        Map<String, Integer> classroomNameToId = new HashMap<>();
        for (Map.Entry<Integer, String> entry : classroomMap.entrySet()) {
            classroomCombo.getItems().add(entry.getValue());
            classroomNameToId.put(entry.getValue(), entry.getKey());
        }

        VBox classroomGroup = makeFieldGroup("Classroom", classroomCombo);
        HBox.setHgrow(classroomGroup, Priority.ALWAYS);

        VBox roleGroup = makeFieldGroup("Role", roleCombo);
        HBox.setHgrow(roleGroup, Priority.ALWAYS);

        HBox roleRow = new HBox();
        roleRow.getStyleClass().add("um-field-row");
        roleRow.getChildren().addAll(roleGroup, classroomGroup);

        // Toggle classroom visibility
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStudent = "STUDENT".equals(newVal);
            classroomGroup.setVisible(isStudent);
            classroomGroup.setManaged(isStudent);
        });

        TextField phoneField = new TextField();
        phoneField.setPromptText("+216 XX XXX XXX");
        phoneField.getStyleClass().add("um-field-input");

        // Enhanced hint with gradient background
        HBox hintBox = new HBox(10);
        hintBox.getStyleClass().add("um-dialog-hint");
        hintBox.setAlignment(Pos.CENTER_LEFT);
        Region hintIcon = new Region();
        hintIcon.getStyleClass().add("um-hint-icon");
        Label hint = new Label("Password will be generated automatically and shown after creation.");
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        hintBox.getChildren().addAll(hintIcon, hint);

        form.getChildren().addAll(
                makeFieldGroup("Full Name", nameField),
                makeFieldGroup("Email Address", emailField),
                roleRow,
                makeFieldGroup("Phone (optional)", phoneField),
                hintBox
        );

        content.getChildren().addAll(headerWrapper, form);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);

        // Disable create button until required fields filled
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createBtnType);
        createButton.setDisable(true);

        Runnable validateFields = () ->
                createButton.setDisable(nameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, o, n) -> validateFields.run());
        emailField.textProperty().addListener((obs, o, n) -> validateFields.run());

        dialog.setResultConverter(btn -> {
            if (btn == createBtnType) {
                User user = new User();
                user.setName(nameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setUserType(roleCombo.getValue());
                user.setStatus("ACTIVE");
                user.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

                if ("STUDENT".equals(roleCombo.getValue()) && classroomCombo.getValue() != null) {
                    user.setClassroomId(classroomNameToId.get(classroomCombo.getValue()));
                }
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            try {
                String generatedPassword = UserService.generatePassword();

                User existing = userService.findByEmail(user.getEmail());
                if (existing != null) {
                    showAlert(Alert.AlertType.ERROR, "Email Already Exists",
                            "A user with email " + user.getEmail() + " already exists.");
                    return;
                }

                userService.create(user, generatedPassword);

                try {
                    Notification notif = new Notification();
                    notif.setUserId(user.getUserId());
                    notif.setTitle("Welcome to Spark");
                    notif.setMessage("Your account has been created. Your initial password is: " + generatedPassword);
                    notif.setType("IN_APP");
                    notif.setStatus("ACTIVE");
                    notificationService.create(notif);
                } catch (SQLException ex) {
                    System.err.println("[UserMgmt] Failed to create notification: " + ex.getMessage());
                }

                showCredentialsDialog(user, generatedPassword);
                loadUsers();

            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // EDIT USER DIALOG — Enhanced with Gradient Header
    // ═══════════════════════════════════════════════════════

    private void openEditUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Spark");

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        applyDialogStyle(dialog.getDialogPane(), "um-dialog-pane");

        // ── Build content ──
        VBox content = new VBox();

        // Enhanced Gradient Header with icon
        StackPane headerWrapper = new StackPane();
        headerWrapper.getStyleClass().add("um-dialog-header");
        
        // Decorative orbs in header
        Region headerOrb1 = new Region();
        headerOrb1.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-1");
        StackPane.setAlignment(headerOrb1, Pos.TOP_RIGHT);
        headerOrb1.setTranslateX(30);
        headerOrb1.setTranslateY(-30);
        headerOrb1.setMouseTransparent(true);
        
        Region headerOrb2 = new Region();
        headerOrb2.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-2");
        StackPane.setAlignment(headerOrb2, Pos.BOTTOM_LEFT);
        headerOrb2.setTranslateX(-20);
        headerOrb2.setTranslateY(15);
        headerOrb2.setMouseTransparent(true);
        
        // Header content
        VBox headerContent = new VBox(6);
        headerContent.setAlignment(Pos.CENTER_LEFT);
        
        // Icon container
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("um-dialog-header-icon");
        Region headerIcon = new Region();
        headerIcon.getStyleClass().add("um-dialog-header-icon-graphic");
        headerIcon.setStyle("-fx-shape: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z';");
        iconContainer.getChildren().add(headerIcon);
        
        Label title = new Label("Edit User");
        title.getStyleClass().add("um-dialog-title");
        Label subtitle = new Label("Modify account details for " + user.getName());
        subtitle.getStyleClass().add("um-dialog-subtitle");
        headerContent.getChildren().addAll(iconContainer, title, subtitle);
        
        headerWrapper.getChildren().addAll(headerOrb1, headerOrb2, headerContent);

        // Form
        VBox form = new VBox();
        form.getStyleClass().add("um-dialog-form");

        TextField nameField = new TextField(user.getName());
        nameField.getStyleClass().add("um-field-input");

        TextField emailField = new TextField(user.getEmail());
        emailField.getStyleClass().add("um-field-input");

        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "STUDENT", "TEACHER", "ADMINISTRATOR"
        ));
        roleCombo.getStyleClass().add("um-field-combo");
        roleCombo.setValue(user.getUserType());
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(roleCombo, Priority.ALWAYS);

        ComboBox<String> classroomCombo = new ComboBox<>();
        classroomCombo.getStyleClass().add("um-field-combo");
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setPromptText("Select classroom");
        HBox.setHgrow(classroomCombo, Priority.ALWAYS);

        Map<String, Integer> classroomNameToId = new HashMap<>();
        for (Map.Entry<Integer, String> entry : classroomMap.entrySet()) {
            classroomCombo.getItems().add(entry.getValue());
            classroomNameToId.put(entry.getValue(), entry.getKey());
        }
        if (user.getClassroomId() != null) {
            classroomCombo.setValue(classroomMap.get(user.getClassroomId()));
        }

        VBox classroomGroup = makeFieldGroup("Classroom", classroomCombo);
        HBox.setHgrow(classroomGroup, Priority.ALWAYS);
        boolean isStudent = "STUDENT".equals(user.getUserType());
        classroomGroup.setVisible(isStudent);
        classroomGroup.setManaged(isStudent);

        VBox roleGroup = makeFieldGroup("Role", roleCombo);
        HBox.setHgrow(roleGroup, Priority.ALWAYS);

        HBox roleRow = new HBox();
        roleRow.getStyleClass().add("um-field-row");
        roleRow.getChildren().addAll(roleGroup, classroomGroup);

        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean show = "STUDENT".equals(newVal);
            classroomGroup.setVisible(show);
            classroomGroup.setManaged(show);
        });

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        statusCombo.getStyleClass().add("um-field-combo");
        statusCombo.setValue(user.getStatus() != null ? user.getStatus() : "ACTIVE");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        TextField phoneField = new TextField(user.getPhone() != null ? user.getPhone() : "");
        phoneField.getStyleClass().add("um-field-input");

        // Status + Phone in a row
        VBox statusGroup = makeFieldGroup("Status", statusCombo);
        HBox.setHgrow(statusGroup, Priority.ALWAYS);
        VBox phoneGroup = makeFieldGroup("Phone", phoneField);
        HBox.setHgrow(phoneGroup, Priority.ALWAYS);

        HBox statusRow = new HBox();
        statusRow.getStyleClass().add("um-field-row");
        statusRow.getChildren().addAll(statusGroup, phoneGroup);

        form.getChildren().addAll(
                makeFieldGroup("Full Name", nameField),
                makeFieldGroup("Email Address", emailField),
                roleRow,
                statusRow
        );

        content.getChildren().addAll(headerWrapper, form);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                user.setName(nameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setUserType(roleCombo.getValue());
                user.setStatus(statusCombo.getValue());
                user.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

                if ("STUDENT".equals(roleCombo.getValue()) && classroomCombo.getValue() != null) {
                    user.setClassroomId(classroomNameToId.get(classroomCombo.getValue()));
                } else {
                    user.setClassroomId(null);
                }
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                userService.update(updated);
                loadUsers();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // PASSWORD RESET
    // ═══════════════════════════════════════════════════════

    private void handleResetPassword(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Generate a new password for " + user.getName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Password Reset");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    String newPassword = UserService.generatePassword();
                    userService.updatePassword(user.getUserId(), newPassword);

                    // Notify user
                    try {
                        Notification notif = new Notification();
                        notif.setUserId(user.getUserId());
                        notif.setTitle("Password Reset");
                        notif.setMessage("Your password has been reset. New password: " + newPassword);
                        notif.setType("IN_APP");
                        notif.setStatus("ACTIVE");
                        notificationService.create(notif);
                    } catch (SQLException ex) {
                        System.err.println("[UserMgmt] Failed to create notification: " + ex.getMessage());
                    }

                    showCredentialsDialog(user, newPassword);
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reset password: " + ex.getMessage());
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // DELETE USER
    // ═══════════════════════════════════════════════════════
    // DELETE USER DIALOG — Enhanced with Danger Theme
    // ═══════════════════════════════════════════════════════

    private void handleDeleteUser(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Spark");

        ButtonType deleteBtnType = new ButtonType("Delete User", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteBtnType, ButtonType.CANCEL);
        applyDialogStyle(dialog.getDialogPane(), "um-delete-dialog-pane");

        VBox content = new VBox();

        // Red gradient header
        StackPane headerWrapper = new StackPane();
        headerWrapper.getStyleClass().add("um-delete-header");

        // Decorative orbs
        Region headerOrb1 = new Region();
        headerOrb1.getStyleClass().add("um-delete-orb-1");
        StackPane.setAlignment(headerOrb1, Pos.TOP_RIGHT);
        headerOrb1.setTranslateX(30);
        headerOrb1.setTranslateY(-30);
        headerOrb1.setMouseTransparent(true);

        Region headerOrb2 = new Region();
        headerOrb2.getStyleClass().add("um-delete-orb-2");
        StackPane.setAlignment(headerOrb2, Pos.BOTTOM_LEFT);
        headerOrb2.setTranslateX(-20);
        headerOrb2.setTranslateY(15);
        headerOrb2.setMouseTransparent(true);

        // Header content
        VBox headerContent = new VBox(6);
        headerContent.setAlignment(Pos.CENTER_LEFT);

        // Delete icon
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("um-delete-header-icon");
        Region headerIcon = new Region();
        headerIcon.getStyleClass().add("um-delete-header-icon-graphic");
        headerIcon.setStyle("-fx-shape: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z';");
        iconContainer.getChildren().add(headerIcon);

        Label title = new Label("Delete User");
        title.getStyleClass().add("um-dialog-title");
        Label subtitle = new Label("This action cannot be undone");
        subtitle.getStyleClass().add("um-dialog-subtitle");
        headerContent.getChildren().addAll(iconContainer, title, subtitle);

        headerWrapper.getChildren().addAll(headerOrb1, headerOrb2, headerContent);

        // Body
        VBox body = new VBox();
        body.getStyleClass().add("um-delete-body");

        // User info card
        VBox userCard = new VBox(8);
        userCard.getStyleClass().add("um-delete-user-card");
        userCard.setAlignment(Pos.CENTER_LEFT);

        HBox userRow = new HBox(12);
        userRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = buildAvatar(user.getName(), user.getUserId());

        VBox userInfo = new VBox(2);
        Label userName = new Label(user.getName());
        userName.getStyleClass().add("um-delete-user-name");
        Label userEmail = new Label(user.getEmail());
        userEmail.getStyleClass().add("um-delete-user-email");
        userInfo.getChildren().addAll(userName, userEmail);

        Label roleBadge = new Label(formatRole(user.getUserType()));
        roleBadge.getStyleClass().addAll("um-delete-user-badge", getRoleBadgeClass(user.getUserType()));

        userRow.getChildren().addAll(avatar, userInfo, roleBadge);
        userCard.getChildren().add(userRow);

        // Warning message
        HBox warningBox = new HBox(10);
        warningBox.getStyleClass().add("um-delete-warning");
        warningBox.setAlignment(Pos.CENTER_LEFT);
        Region warningIcon = new Region();
        warningIcon.getStyleClass().add("um-delete-warning-icon");
        Label warning = new Label("All data associated with this user will be permanently removed from the system.");
        warning.setStyle("-fx-text-fill: #991B1B; -fx-font-size: 13px; -fx-font-weight: 500;");
        warning.setWrapText(true);
        warningBox.getChildren().addAll(warningIcon, warning);

        body.getChildren().addAll(userCard, warningBox);
        content.getChildren().addAll(headerWrapper, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(440);

        dialog.setResultConverter(btn -> btn == deleteBtnType);

        dialog.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                try {
                    userService.delete(user.getUserId());
                    loadUsers();
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + ex.getMessage());
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════

    private void showCredentialsDialog(User user, String password) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Spark");

        ButtonType doneBtnType = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(doneBtnType);
        applyDialogStyle(dialog.getDialogPane(), "um-cred-dialog-pane");

        VBox content = new VBox();

        // Enhanced Success header with gradient
        StackPane headerWrapper = new StackPane();
        headerWrapper.getStyleClass().add("um-cred-header");
        
        // Decorative orbs in header
        Region headerOrb1 = new Region();
        headerOrb1.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-1");
        StackPane.setAlignment(headerOrb1, Pos.TOP_RIGHT);
        headerOrb1.setTranslateX(30);
        headerOrb1.setTranslateY(-30);
        headerOrb1.setMouseTransparent(true);
        
        Region headerOrb2 = new Region();
        headerOrb2.getStyleClass().addAll("um-dialog-orb", "um-dialog-orb-2");
        StackPane.setAlignment(headerOrb2, Pos.BOTTOM_LEFT);
        headerOrb2.setTranslateX(-20);
        headerOrb2.setTranslateY(15);
        headerOrb2.setMouseTransparent(true);
        
        // Header content
        VBox headerContent = new VBox(8);
        headerContent.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("um-cred-icon");
        iconCircle.setMinSize(52, 52);
        iconCircle.setPrefSize(52, 52);
        iconCircle.setMaxSize(52, 52);
        Label checkMark = new Label("\u2713");
        checkMark.getStyleClass().add("um-cred-icon-text");
        iconCircle.getChildren().add(checkMark);

        Label title = new Label("Account Created Successfully");
        title.getStyleClass().add("um-cred-title");
        Label subtitle = new Label("Share these credentials securely with " + user.getName());
        subtitle.getStyleClass().add("um-cred-subtitle");
        headerContent.getChildren().addAll(iconCircle, title, subtitle);
        
        headerWrapper.getChildren().addAll(headerOrb1, headerOrb2, headerContent);

        // Credential card
        VBox body = new VBox();
        body.getStyleClass().add("um-cred-body");

        VBox credCard = new VBox();
        credCard.getStyleClass().add("um-cred-card");

        HBox emailRow = new HBox();
        emailRow.getStyleClass().add("um-cred-row");
        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("um-cred-label");
        Label emailValue = new Label(user.getEmail());
        emailValue.getStyleClass().add("um-cred-value");
        emailRow.getChildren().addAll(emailLabel, emailValue);

        HBox pwdRow = new HBox();
        pwdRow.getStyleClass().add("um-cred-row");
        Label pwdLabel = new Label("Password");
        pwdLabel.getStyleClass().add("um-cred-label");
        Label pwdValue = new Label(password);
        pwdValue.getStyleClass().add("um-cred-password");
        pwdRow.getChildren().addAll(pwdLabel, pwdValue);

        HBox roleRow = new HBox();
        roleRow.getStyleClass().add("um-cred-row");
        Label roleLabel = new Label("Role");
        roleLabel.getStyleClass().add("um-cred-label");
        Label roleValue = new Label(formatRole(user.getUserType()));
        roleValue.getStyleClass().add("um-cred-value");
        roleRow.getChildren().addAll(roleLabel, roleValue);

        credCard.getChildren().addAll(emailRow, pwdRow, roleRow);

        // Enhanced warning with icon
        HBox warningBox = new HBox(10);
        warningBox.getStyleClass().add("um-cred-warning");
        warningBox.setAlignment(Pos.CENTER_LEFT);
        Region warningIcon = new Region();
        warningIcon.getStyleClass().add("um-warning-icon");
        Label warning = new Label("This password will not be shown again. Please save it now.");
        warning.setStyle("-fx-text-fill: #92400E; -fx-font-size: 13px; -fx-font-weight: 500;");
        warningBox.getChildren().addAll(warningIcon, warning);
        warningBox.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(credCard, warningBox);
        content.getChildren().addAll(headerWrapper, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatRole(String userType) {
        return switch (userType) {
            case "ADMINISTRATOR" -> "Admin";
            case "TEACHER" -> "Teacher";
            case "STUDENT" -> "Student";
            default -> userType;
        };
    }

    private String getRoleBadgeClass(String userType) {
        return switch (userType) {
            case "ADMINISTRATOR" -> "um-badge-admin";
            case "TEACHER" -> "um-badge-teacher";
            case "STUDENT" -> "um-badge-student";
            default -> "um-badge-student";
        };
    }
}
