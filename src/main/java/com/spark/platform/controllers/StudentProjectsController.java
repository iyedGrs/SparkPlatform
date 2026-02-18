package com.spark.platform.controllers;

import com.spark.platform.models.Project;
import com.spark.platform.services.StudentProjectService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Student Projects view - allows student to select which project to work on.
 * Opens the Project Board (kanban) for the selected project.
 */
public class StudentProjectsController {

    // ──── FXML bindings ────
    @FXML private Label viewTitle;
    @FXML private Label viewSubtitle;
    @FXML private StackPane contentArea;

    // ──── Service ────
    private final StudentProjectService service = new StudentProjectService();

    // ──── State ────
    // TODO: HARDCODED — replace with actual student ID from session/RBAC
    private static final int CURRENT_STUDENT_ID = 5;

    // ──── Init ────
    @FXML
    private void initialize() {
        loadProjects();
    }

    private void loadProjects() {
        viewTitle.setText("Project Board");
        viewSubtitle.setText("Select a project to open its kanban board");

        contentArea.getChildren().clear();

        try {
            List<Project> projects = service.findProjectsByStudent(CURRENT_STUDENT_ID);

            VBox container = new VBox(24);
            container.getStyleClass().add("sp-container");
            container.setPadding(new Insets(24));
            StackPane.setAlignment(container, Pos.TOP_LEFT);

            if (projects.isEmpty()) {
                Label emptyLabel = new Label("You are not assigned to any projects yet.");
                emptyLabel.getStyleClass().add("sp-empty");
                container.getChildren().add(emptyLabel);
            } else {
                // Create project cards grid
                GridPane grid = new GridPane();
                grid.getStyleClass().add("sp-grid");
                grid.setHgap(16);
                grid.setVgap(16);

                int col = 0;
                int row = 0;

                for (Project p : projects) {
                    VBox card = buildProjectCard(p);
                    grid.add(card, col, row);

                    col++;
                    if (col >= 3) {
                        col = 0;
                        row++;
                    }
                }

                container.getChildren().add(grid);
            }

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("sp-scroll");
            contentArea.getChildren().add(scroll);

        } catch (SQLException e) {
            showError("Failed to load projects: " + e.getMessage());
        }
    }

    private VBox buildProjectCard(Project p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("sp-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setMinHeight(200);

        // Title
        Label title = new Label(p.getTitle());
        title.getStyleClass().add("sp-card-title");
        title.setWrapText(true);

        // Description
        Label desc = new Label(p.getDescription() != null ? p.getDescription() : "");
        desc.getStyleClass().add("sp-card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        // Info row (template type + member count)
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(p.getTemplateType() != null ? p.getTemplateType() : "STANDARD");
        typeBadge.getStyleClass().addAll("sp-badge", "sp-badge-type");

        try {
            int memberCount = service.countMembers(p.getProjectId());
            Label membersBadge = new Label(memberCount + " member" + (memberCount != 1 ? "s" : ""));
            membersBadge.getStyleClass().addAll("sp-badge", "sp-badge-members");
            infoRow.getChildren().addAll(typeBadge, membersBadge);
        } catch (SQLException e) {
            infoRow.getChildren().add(typeBadge);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Open button
        Button openBtn = new Button("Open Kanban Board");
        openBtn.getStyleClass().add("sp-open-btn");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setOnAction(e -> openProjectBoard(p));

        card.getChildren().addAll(title, desc, infoRow, spacer, openBtn);
        return card;
    }

    private void openProjectBoard(Project project) {
        try {
            // Load the Project Board controller and set the project
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project-board-view.fxml"));
            VBox boardView = loader.load();
            
            ProjectBoardController boardController = loader.getController();
            boardController.setProject(project.getProjectId());
            
            // Find the content outlet in AppShell from the scene root
            Node root = contentArea.getScene().getRoot();
            StackPane contentOutlet = (StackPane) root.lookup("#contentOutlet");
            
            if (contentOutlet != null) {
                contentOutlet.getChildren().clear();
                contentOutlet.getChildren().add(boardView);
                
                // Update page title
                Label pageTitle = (Label) root.lookup("#pageTitle");
                if (pageTitle != null) {
                    pageTitle.setText(project.getTitle() + " — Kanban Board");
                }
            } else {
                showError("Could not find content outlet. Root: " + root);
            }
        } catch (Exception e) {
            showError("Failed to open project board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }
}
