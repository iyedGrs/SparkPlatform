package com.spark.platform.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the shared app-shell layout.
 *
 * <h3>How other devs plug in their module</h3>
 * <ol>
 *   <li>Create your module FXML under {@code /fxml/} (e.g. {@code grades-view.fxml})</li>
 *   <li>Register it in {@link #PAGE_META} with a matching button fx:id key</li>
 *   <li>The shell loads your FXML into the center outlet and updates the topbar title</li>
 * </ol>
 */
public class AppShellController {

    // ──── FXML bindings ────
    @FXML private StackPane contentOutlet;
    @FXML private Label     pageTitle;
    @FXML private Label     pageSubtitle;

    @FXML private VBox navGroupCore;
    @FXML private VBox navGroupLabs;

    // ──── Page registry ────
    // Each entry: nav-button fx:id  ->  { fxmlPath, title, subtitle }
    private static final Map<String, String[]> PAGE_META = new HashMap<>();

    static {
        //  fx:id               FXML resource path                   Title              Subtitle
        PAGE_META.put("navDashboard",     new String[]{null,                            "Dashboard",       "Your academic cockpit"});
        PAGE_META.put("navClassroom",     new String[]{"/fxml/classroom-view.fxml",     "Classroom",       "Courses & materials"});
        PAGE_META.put("navGrades",        new String[]{"/fxml/grades-view.fxml",        "Grades",          "Track your performance"});
        PAGE_META.put("navScheduler",     new String[]{"/fxml/scheduler-view.fxml",     "Scheduler",       "Plan your week"});
        PAGE_META.put("navSparkyAI",      new String[]{"/fxml/sparky-ai-view.fxml",     "Sparky AI",       "Your study assistant"});
        PAGE_META.put("navProjectBoard",  new String[]{"/fxml/project-board-view.fxml", "Project Board",   "Agile project management"});
        PAGE_META.put("navOpportunities", new String[]{"/fxml/opportunities-view.fxml", "Opportunities",   "Internships & careers"});
    }

    private Button activeNavButton;

    // ──── Initialization ────
    @FXML
    private void initialize() {
        // Mark "Dashboard" as the default active button
        if (navGroupCore != null && !navGroupCore.getChildren().isEmpty()) {
            activeNavButton = (Button) navGroupCore.getChildren().get(0);
        }
    }

    // ──── Navigation handler (every sidebar button calls this) ────
    @FXML
    private void onNavClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        if (clicked == activeNavButton) return;          // already there

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

        // Update topbar
        pageTitle.setText(meta[1]);
        pageSubtitle.setText(meta[2]);

        // Load module FXML into outlet (or show default welcome for Dashboard)
        loadIntoOutlet(meta[0]);
    }

    /**
     * Loads a module FXML into the center outlet.
     * Pass {@code null} to restore the default welcome card.
     */
    public void loadIntoOutlet(String fxmlPath) {
        contentOutlet.getChildren().clear();

        if (fxmlPath == null) {
            showDefaultWelcome();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentOutlet.getChildren().add(view);
        } catch (IOException e) {
            // Module FXML not created yet — show a placeholder
            showPlaceholder(pageTitle.getText());
        }
    }

    /** The welcome card shown on Dashboard / first launch. */
    private void showDefaultWelcome() {
        Label title = new Label("Welcome to Spark");
        title.getStyleClass().add("card-title");

        Label body = new Label("This shared shell keeps navigation consistent.\n"
                + "Each module view loads into this outlet area.");
        body.getStyleClass().add("card-body");
        body.setWrapText(true);

        VBox card = new VBox(12, title, body);
        card.getStyleClass().add("content-card");
        card.setMaxWidth(760);

        contentOutlet.getChildren().add(card);
    }

    /** Placeholder shown when a module FXML hasn't been created yet. */
    private void showPlaceholder(String moduleName) {
        Label title = new Label(moduleName);
        title.getStyleClass().add("card-title");

        Label body = new Label("This module is under construction.\n"
                + "The developer assigned to this module will add the view here.");
        body.getStyleClass().add("card-body");
        body.setWrapText(true);

        Label pill = new Label("Coming soon");
        pill.getStyleClass().add("pill");

        VBox card = new VBox(12, title, body, pill);
        card.getStyleClass().add("content-card");
        card.setMaxWidth(760);

        contentOutlet.getChildren().add(card);
    }
}
