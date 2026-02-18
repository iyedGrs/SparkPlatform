package com.spark.platform.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the shared app-shell layout.
 * Matches the spark demo design: 4 nav items + settings, navbar with page title.
 */
public class AppShellController {

    // ──── FXML bindings ────
    @FXML private StackPane contentOutlet;
    @FXML private Label     pageTitle;
    @FXML private Label     avatarInitials;
    @FXML private VBox      navGroup;

    // ──── Page registry ────
    private static final Map<String, String[]> PAGE_META = new HashMap<>();

    static {
        //  fx:id               FXML resource path                   Title
        PAGE_META.put("navClassroom",     new String[]{"/fxml/classroom-view.fxml",     "Classroom"});
        PAGE_META.put("navSparkAI",       new String[]{"/fxml/spark-ai-view.fxml",      "Spark AI"});
        PAGE_META.put("navProjectBoard",  new String[]{"/fxml/student-projects-view.fxml", "Project Board"});
        PAGE_META.put("navOpportunities", new String[]{"/fxml/opportunities-view.fxml", "Opportunities"});
        PAGE_META.put("navTeacherProjects", new String[]{"/fxml/teacher-projects-view.fxml", "Teacher Projects"});
        PAGE_META.put("navSettings",      new String[]{"/fxml/settings-view.fxml",      "Settings"});
    }

    private Button activeNavButton;

    // ──── Initialization ────
    @FXML
    private void initialize() {
        // Default: Project Board is active (matches the FXML where it has "active" class)
        if (navGroup != null) {
            for (Node child : navGroup.getChildren()) {
                if (child instanceof Button btn && "navProjectBoard".equals(btn.getId())) {
                    activeNavButton = btn;
                    break;
                }
            }
        }
        // Load the default page (Project Board)
        String[] defaultMeta = PAGE_META.get("navProjectBoard");
        if (defaultMeta != null) {
            pageTitle.setText(defaultMeta[1]);
            loadIntoOutlet(defaultMeta[0]);
        } else {
            showPlaceholder("Project Board");
        }
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

    // ──── Profile dropdown handler ────
    @FXML
    private void onProfileClick(ActionEvent event) {
        // TODO: Show profile dropdown (Profile, Account, Logout)
    }

    /**
     * Loads a module FXML into the center outlet.
     * Pass null to clear.
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
