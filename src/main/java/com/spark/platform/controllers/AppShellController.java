package com.spark.platform.controllers;

import com.spark.platform.MainApp;
import com.spark.platform.models.User;
import com.spark.platform.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the shared app-shell layout.
 * Handles role-based navigation and dynamic profile display.
 */
public class AppShellController {

    // ──── FXML bindings ────
    @FXML private StackPane contentOutlet;
    @FXML private Label     pageTitle;
    @FXML private Label     avatarInitials;
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileRoleLabel;
    @FXML private VBox      navGroup;

    // ──── Page registry ────
    private static final Map<String, String[]> PAGE_META = new HashMap<>();

    static {
        //  fx:id               FXML resource path                            Title
        PAGE_META.put("navClassroom",       new String[]{"/fxml/classroom-view.fxml",        "Classroom"});
        PAGE_META.put("navSparkAI",         new String[]{"/fxml/spark-ai-view.fxml",         "Spark AI"});
        PAGE_META.put("navProjectBoard",    new String[]{"/fxml/student-projects-view.fxml", "Project Board"});
        PAGE_META.put("navOpportunities",   new String[]{"/fxml/opportunities-view.fxml",    "Opportunities"});
        PAGE_META.put("navTeacherProjects", new String[]{"/fxml/teacher-projects-view.fxml", "Teacher Projects"});
        PAGE_META.put("navUserManagement",  new String[]{"/fxml/user-management-view.fxml",  "User Management"});
        PAGE_META.put("navSettings",        new String[]{"/fxml/settings-view.fxml",         "Settings"});
    }

    private Button activeNavButton;

    // ──── Initialization ────
    @FXML
    private void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        // Update profile display with real user data
        if (currentUser != null) {
            updateProfileDisplay(currentUser);
            configureNavForRole(currentUser.getUserType());
        }

        // Determine default page based on role
        String defaultNavId = getDefaultNavId(currentUser);

        // Find and activate the default nav button
        if (navGroup != null) {
            for (Node child : navGroup.getChildren()) {
                if (child instanceof Button btn && defaultNavId.equals(btn.getId())) {
                    activeNavButton = btn;
                    if (!btn.getStyleClass().contains("active")) {
                        btn.getStyleClass().add("active");
                    }
                    break;
                }
            }
            // Remove "active" from non-default buttons
            for (Node child : navGroup.getChildren()) {
                if (child instanceof Button btn && btn != activeNavButton) {
                    btn.getStyleClass().remove("active");
                }
            }
        }

        // Load the default page
        String[] defaultMeta = PAGE_META.get(defaultNavId);
        if (defaultMeta != null) {
            pageTitle.setText(defaultMeta[1]);
            loadIntoOutlet(defaultMeta[0]);
        } else {
            showPlaceholder("Classroom");
        }
    }

    private void updateProfileDisplay(User user) {
        // Avatar initials
        if (avatarInitials != null && user.getName() != null) {
            String[] parts = user.getName().split("\\s+");
            String initials = String.valueOf(parts[0].charAt(0));
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
            avatarInitials.setText(initials.toUpperCase());
        }

        // Profile labels
        if (profileNameLabel != null) {
            profileNameLabel.setText(user.getName());
        }
        if (profileRoleLabel != null) {
            String roleDisplay = switch (user.getUserType()) {
                case "ADMINISTRATOR" -> "Admin";
                case "TEACHER" -> "Teacher";
                case "STUDENT" -> "Student";
                default -> user.getUserType();
            };
            profileRoleLabel.setText(roleDisplay);
        }
    }

    private void configureNavForRole(String userType) {
        if (navGroup == null) return;

        for (Node child : navGroup.getChildren()) {
            if (!(child instanceof Button btn)) continue;
            String id = btn.getId();
            if (id == null) continue;

            switch (userType) {
                case "ADMINISTRATOR" -> {
                    // Admin sees: Classroom, SparkAI, UserManagement, Opportunities
                    // Show User Management (starts hidden in FXML)
                    if ("navUserManagement".equals(id)) {
                        btn.setVisible(true);
                        btn.setManaged(true);
                    }
                    // Hide student/teacher project views
                    if ("navProjectBoard".equals(id) || "navTeacherProjects".equals(id)) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    }
                }
                case "TEACHER" -> {
                    // Teacher sees: Classroom, SparkAI, TeacherProjects, Opportunities
                    // Hide admin and student-specific views
                    if ("navUserManagement".equals(id) || "navProjectBoard".equals(id)) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    }
                }
                case "STUDENT" -> {
                    // Student sees: Classroom, SparkAI, ProjectBoard, Opportunities
                    // Hide admin and teacher-specific views
                    if ("navUserManagement".equals(id) || "navTeacherProjects".equals(id)) {
                        btn.setVisible(false);
                        btn.setManaged(false);
                    }
                }
            }
        }
    }

    private String getDefaultNavId(User user) {
        if (user != null && "ADMINISTRATOR".equals(user.getUserType())) {
            return "navUserManagement";
        }
        return "navClassroom";
    }

    // ──── Navigation handler ────
    @FXML
    private void onNavClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        if (clicked == activeNavButton) return;

        // Swap active styling
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("active");
        }
        clicked.getStyleClass().add("active");
        activeNavButton = clicked;

        // Look up page metadata
        String id = clicked.getId();
        String[] meta = PAGE_META.get(id);
        if (meta == null) return;

        // Update navbar title
        pageTitle.setText(meta[1]);

        // Load module FXML into outlet
        loadIntoOutlet(meta[0]);
    }

    // ──── Profile / Logout handler ────
    @FXML
    private void onProfileClick(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to log out?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Spark");
        confirm.setHeaderText("Logout");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                SessionManager.getInstance().logout();
                MainApp.showLoginPage();
            }
        });
    }

    /**
     * Loads a module FXML into the center outlet.
     */
    public void loadIntoOutlet(String fxmlPath) {
        contentOutlet.getChildren().clear();

        if (fxmlPath == null) return;

        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showPlaceholder(pageTitle.getText());
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentOutlet.getChildren().add(view);
        } catch (Exception e) {
            showPlaceholder(pageTitle.getText());
        }
    }

    /** Placeholder shown when a module FXML hasn't been created yet. */
    private void showPlaceholder(String moduleName) {
        contentOutlet.getChildren().clear();

        Label title = new Label(moduleName);
        title.getStyleClass().add("placeholder-title");

        Label body = new Label("Coming Soon");
        body.getStyleClass().add("placeholder-body");

        VBox container = new VBox(8, title, body);
        container.getStyleClass().add("placeholder-container");
        container.setMaxWidth(400);

        contentOutlet.getChildren().add(container);
    }
}
