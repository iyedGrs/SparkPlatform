package com.spark.platform.controllers;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Sprint;
import com.spark.platform.models.Task;
import com.spark.platform.models.User;
import com.spark.platform.services.SprintService;
import com.spark.platform.services.TaskService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Project Board Kanban view.
 * Handles: CRUD operations on Tasks via TaskService,
 *           Sprint selection, Kanban column rendering,
 *           search/filter, task detail panel with input validation.
 */
public class ProjectBoardController {

    // ──── FXML bindings ────
    @FXML private ComboBox<Sprint> sprintSelector;
    @FXML private Label sprintDates;
    @FXML private Label sprintStatus;
    @FXML private HBox teamAvatars;
    @FXML private Label statsLabel;

    @FXML private Button tabBoard;
    @FXML private Button tabBacklog;
    @FXML private Button tabTimeline;
    @FXML private Button tabActivity;

    @FXML private HBox filterBar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private ComboBox<String> assigneeFilter;

    @FXML private ScrollPane boardScroll;
    @FXML private HBox kanbanColumns;
    @FXML private HBox detailOverlay;

    @FXML private ScrollPane backlogScroll;
    @FXML private VBox backlogContent;

    // ──── Services ────
    private final TaskService taskService = new TaskService();
    private final SprintService sprintService = new SprintService();

    // ──── State ────
    private static final int PROJECT_ID = 1; // current project
    private final String[] COLUMNS = {"TODO", "IN_PROGRESS", "REVIEW", "DONE"};
    private final Map<String, String> COLUMN_LABELS = new LinkedHashMap<>();
    private List<Task> allTasks = new ArrayList<>();
    private List<User> teamMembers = new ArrayList<>();
    private Button activeTab;

    // Backlog state
    private List<Task> backlogTasks = new ArrayList<>();
    private final Set<Integer> expandedBacklogTasks = new HashSet<>();

    // Drag-and-drop data format
    private static final DataFormat TASK_ID_FORMAT = new DataFormat("application/x-spark-task-id");

    // Avatar colors palette
    private static final String[] AVATAR_COLORS = {
        "#BFDBFE", "#BBF7D0", "#FDE68A", "#FECACA", "#DDD6FE",
        "#C7D2FE", "#A7F3D0", "#FED7AA"
    };

    // ──── Init ────
    @FXML
    private void initialize() {
        COLUMN_LABELS.put("TODO",        "TO DO");
        COLUMN_LABELS.put("IN_PROGRESS", "IN PROGRESS");
        COLUMN_LABELS.put("REVIEW",      "IN REVIEW");
        COLUMN_LABELS.put("DONE",        "DONE");

        activeTab = tabBoard;

        // Setup sprint selector display
        sprintSelector.setConverter(new StringConverter<Sprint>() {
            @Override public String toString(Sprint s) {
                return s == null ? "" : "Sprint " + s.getSprintNumber() + " — " + s.getTitle();
            }
            @Override public Sprint fromString(String s) { return null; }
        });

        // Setup priority filter
        priorityFilter.setItems(FXCollections.observableArrayList(
            "All", "CRITICAL", "HIGH", "MEDIUM", "LOW"
        ));
        priorityFilter.getSelectionModel().selectFirst();

        // Listen for search text changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderActiveTab());

        // Load data
        loadTeamMembers();
        loadSprints();
    }

    /** Populate the assignee filter after team members are loaded. */
    private void setupAssigneeFilter() {
        List<String> items = new ArrayList<>();
        items.add("All");
        for (User u : teamMembers) {
            items.add(u.getName());
        }
        assigneeFilter.setItems(FXCollections.observableArrayList(items));
        assigneeFilter.getSelectionModel().selectFirst();
    }

    // ──── Data loading ────
    private void loadSprints() {
        try {
            List<Sprint> sprints = sprintService.findByProject(PROJECT_ID);
            sprintSelector.setItems(FXCollections.observableArrayList(sprints));

            // Select the ACTIVE sprint by default, or the first one
            Sprint active = sprints.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .findFirst()
                .orElse(sprints.isEmpty() ? null : sprints.get(0));

            if (active != null) {
                sprintSelector.getSelectionModel().select(active);
                updateSprintInfo(active);
                loadTasks(active.getSprintId());
            }
        } catch (SQLException e) {
            showError("Failed to load sprints: " + e.getMessage());
        }
    }

    private void loadTasks(Integer sprintId) {
        try {
            allTasks = taskService.findByProjectAndSprint(PROJECT_ID, sprintId);
            renderBoard();
        } catch (SQLException e) {
            showError("Failed to load tasks: " + e.getMessage());
        }
    }

    private void loadTeamMembers() {
        teamMembers.clear();
        String sql = "SELECT u.user_id, u.name, u.email FROM users u " +
                     "INNER JOIN project_members pm ON u.user_id = pm.user_id " +
                     "WHERE pm.project_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, PROJECT_ID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    teamMembers.add(u);
                }
            }
        } catch (SQLException e) {
            // Continue without team members
        }
        renderTeamAvatars();
        setupAssigneeFilter();
    }

    private void renderTeamAvatars() {
        teamAvatars.getChildren().clear();
        int i = 0;
        for (User u : teamMembers) {
            StackPane avatar = buildAvatar(u.getName(), 28, AVATAR_COLORS[i % AVATAR_COLORS.length]);
            teamAvatars.getChildren().add(avatar);
            i++;
        }
    }

    // ──── Sprint change ────
    @FXML
    private void onSprintChanged(ActionEvent event) {
        Sprint selected = sprintSelector.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        updateSprintInfo(selected);
        loadTasks(selected.getSprintId());
    }

    private void updateSprintInfo(Sprint sprint) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
        String dates = "";
        if (sprint.getStartDate() != null && sprint.getEndDate() != null) {
            dates = sdf.format(sprint.getStartDate()) + " – " + sdf.format(sprint.getEndDate());
        }
        sprintDates.setText(dates);
        sprintStatus.setText(sprint.getStatus() != null ? sprint.getStatus() : "");
    }

    // ──── Tab switching ────
    @FXML
    private void onTabClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        if (clicked == activeTab) return;

        activeTab.getStyleClass().remove("active");
        clicked.getStyleClass().add("active");
        activeTab = clicked;

        boolean isBoard = clicked == tabBoard;
        boolean isBacklog = clicked == tabBacklog;

        // Show/hide filter bar for board and backlog
        filterBar.setVisible(isBoard);
        filterBar.setManaged(isBoard);

        // Show/hide board scroll
        boardScroll.setVisible(isBoard);
        boardScroll.setManaged(isBoard);

        // Show/hide backlog scroll
        backlogScroll.setVisible(isBacklog);
        backlogScroll.setManaged(isBacklog);

        if (isBoard) {
            renderBoard();
        } else if (isBacklog) {
            renderBacklog();
        } else {
            showTabPlaceholder(clicked.getText());
        }
    }

    private void showTabPlaceholder(String tabName) {
        // Hide board and backlog, show placeholder in a visible panel
        boardScroll.setVisible(true);
        boardScroll.setManaged(true);
        kanbanColumns.getChildren().clear();
        VBox placeholder = new VBox(8);
        placeholder.getStyleClass().add("board-placeholder");
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPrefWidth(600);

        Label title = new Label(tabName);
        title.getStyleClass().add("board-placeholder-title");
        Label body = new Label("This tab is coming soon.");
        body.getStyleClass().add("board-placeholder-text");

        placeholder.getChildren().addAll(title, body);
        kanbanColumns.getChildren().add(placeholder);
    }

    // ══════════════════════════════════════════════════════
    //   BACKLOG VIEW
    // ══════════════════════════════════════════════════════

    private void loadBacklogTasks() {
        try {
            backlogTasks = taskService.findBacklog(PROJECT_ID);
        } catch (SQLException e) {
            showError("Failed to load backlog: " + e.getMessage());
        }
    }

    private void renderBacklog() {
        loadBacklogTasks();
        backlogContent.getChildren().clear();

        // ─── Toolbar ───
        HBox toolbar = buildBacklogToolbar();
        backlogContent.getChildren().add(toolbar);

        // ─── List ───
        refreshBacklogList();
    }

    private void refreshBacklogList() {
        // Remove everything except the toolbar (first child)
        if (backlogContent.getChildren().size() > 1) {
            backlogContent.getChildren().remove(1, backlogContent.getChildren().size());
        }

        List<Task> filtered = getFilteredBacklogTasks();

        // Update count badge
        updateBacklogCount(filtered.size(), backlogTasks.size());

        if (filtered.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("backlog-empty");
            Label emptyTitle = new Label("No backlog items");
            emptyTitle.getStyleClass().add("backlog-empty-title");
            Label emptyText = new Label(backlogTasks.isEmpty()
                ? "Create tasks without a sprint to populate the backlog."
                : "No items match your current filters.");
            emptyText.getStyleClass().add("backlog-empty-text");
            empty.getChildren().addAll(emptyTitle, emptyText);
            backlogContent.getChildren().add(empty);
            return;
        }

        // Bordered container for the list
        VBox listBorder = new VBox();
        listBorder.getStyleClass().add("backlog-list-border");
        listBorder.setPadding(new Insets(0));

        VBox listInner = new VBox();
        listInner.getStyleClass().add("backlog-list");
        listInner.setPadding(new Insets(0));
        listInner.setSpacing(0);

        for (int i = 0; i < filtered.size(); i++) {
            Task task = filtered.get(i);
            boolean isLast = (i == filtered.size() - 1);
            boolean isExpanded = expandedBacklogTasks.contains(task.getTaskId());

            VBox rowGroup = buildBacklogRow(task, isLast, isExpanded);
            listInner.getChildren().add(rowGroup);
        }

        listBorder.getChildren().add(listInner);

        VBox listWrapper = new VBox(listBorder);
        listWrapper.setPadding(new Insets(0, 24, 16, 24));

        backlogContent.getChildren().add(listWrapper);
    }

    private Label backlogCountBadge; // persists for updates

    private HBox buildBacklogToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("backlog-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Count badge
        backlogCountBadge = new Label("");
        backlogCountBadge.getStyleClass().add("backlog-count-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create Task button
        Button createBtn = new Button("Create Task");
        createBtn.getStyleClass().add("backlog-create-btn");

        Region addIcon = new Region();
        addIcon.getStyleClass().add("add-icon");
        addIcon.setStyle("-fx-background-color: white;");
        createBtn.setGraphic(addIcon);
        createBtn.setContentDisplay(ContentDisplay.LEFT);

        createBtn.setOnAction(e -> openCreateBacklogTaskDialog());

        toolbar.getChildren().addAll(backlogCountBadge, spacer, createBtn);
        return toolbar;
    }

    private void updateBacklogCount(int filteredCount, int totalCount) {
        if (backlogCountBadge != null) {
            backlogCountBadge.setText(filteredCount + " of " + totalCount + " items");
        }
    }

    private VBox buildBacklogRow(Task task, boolean isLast, boolean isExpanded) {
        VBox rowGroup = new VBox();

        // ─── Main row ───
        HBox row = new HBox(10);
        row.getStyleClass().add("backlog-row");
        if (isLast && !isExpanded) {
            row.getStyleClass().add("backlog-row-last");
        }
        row.setAlignment(Pos.CENTER_LEFT);

        // Chevron
        Label chevron = new Label(isExpanded ? "▼" : "▶");
        chevron.getStyleClass().add("backlog-chevron");

        // Priority dot
        Region priorityDot = new Region();
        priorityDot.getStyleClass().addAll("priority-dot", getPriorityClass(task.getPriority()));

        // Task ID
        Label idLabel = new Label("TASK-" + task.getTaskId());
        idLabel.getStyleClass().add("backlog-task-id");

        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("backlog-task-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Priority chip
        Label chip = buildLabelChip(task);

        // Story points
        HBox rightSide = new HBox(8);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        if (task.getEstimatedHours() != null) {
            Label pointsBadge = new Label(String.valueOf(task.getEstimatedHours().intValue()));
            pointsBadge.getStyleClass().add("story-points-badge");
            rightSide.getChildren().add(pointsBadge);
        }

        // Assignee avatar
        if (task.getAssignedTo() != null) {
            User assignee = teamMembers.stream()
                .filter(u -> u.getUserId() == task.getAssignedTo())
                .findFirst().orElse(null);
            String name = assignee != null ? assignee.getName() : "?";
            int idx = teamMembers.indexOf(assignee);
            String color = AVATAR_COLORS[Math.max(0, idx) % AVATAR_COLORS.length];
            StackPane avatar = buildAvatar(name, 24, color);
            rightSide.getChildren().add(avatar);
        }

        row.getChildren().addAll(chevron, priorityDot, idLabel, titleLabel);
        if (chip != null) row.getChildren().add(chip);
        row.getChildren().add(rightSide);

        // Click to toggle expand
        row.setOnMouseClicked(e -> {
            if (expandedBacklogTasks.contains(task.getTaskId())) {
                expandedBacklogTasks.remove(task.getTaskId());
            } else {
                expandedBacklogTasks.add(task.getTaskId());
            }
            refreshBacklogList();
        });

        rowGroup.getChildren().add(row);

        // ─── Expanded content ───
        if (isExpanded) {
            VBox expanded = new VBox(6);
            expanded.getStyleClass().add("backlog-expanded");
            if (isLast) {
                expanded.setStyle("-fx-border-width: 0;");
            }

            String desc = task.getDescription() != null && !task.getDescription().isBlank()
                ? task.getDescription()
                : "No description provided.";
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("backlog-desc");
            descLabel.setWrapText(true);

            // "Move to current sprint" button
            Sprint currentSprint = sprintSelector.getSelectionModel().getSelectedItem();
            if (currentSprint != null) {
                Button moveBtn = new Button("Move to Sprint " + currentSprint.getSprintNumber() + " \u2192");
                moveBtn.getStyleClass().add("backlog-move-btn");
                moveBtn.setOnAction(ev -> {
                    try {
                        taskService.moveToSprint(task.getTaskId(), currentSprint.getSprintId());
                        backlogTasks.remove(task);
                        expandedBacklogTasks.remove(task.getTaskId());
                        refreshBacklogList();
                        // Also refresh board tasks if we switch back
                        loadTasks(currentSprint.getSprintId());
                    } catch (SQLException ex) {
                        showError("Failed to move task: " + ex.getMessage());
                    }
                });
                expanded.getChildren().addAll(descLabel, moveBtn);
            } else {
                expanded.getChildren().add(descLabel);
            }

            rowGroup.getChildren().add(expanded);
        }

        return rowGroup;
    }

    private List<Task> getFilteredBacklogTasks() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String priorityVal = priorityFilter.getSelectionModel().getSelectedItem();
        String assigneeVal = assigneeFilter.getSelectionModel().getSelectedItem();

        Integer assigneeId = null;
        if (assigneeVal != null && !"All".equals(assigneeVal)) {
            assigneeId = teamMembers.stream()
                .filter(u -> u.getName().equals(assigneeVal))
                .map(User::getUserId)
                .findFirst().orElse(null);
        }
        final Integer filteredAssigneeId = assigneeId;

        return backlogTasks.stream()
            .filter(t -> {
                if (!searchText.isEmpty()) {
                    boolean matchTitle = t.getTitle() != null && t.getTitle().toLowerCase().contains(searchText);
                    boolean matchId = ("TASK-" + t.getTaskId()).toLowerCase().contains(searchText);
                    if (!matchTitle && !matchId) return false;
                }
                if (priorityVal != null && !"All".equals(priorityVal)) {
                    if (!priorityVal.equals(t.getPriority())) return false;
                }
                if (filteredAssigneeId != null) {
                    if (t.getAssignedTo() == null || !filteredAssigneeId.equals(t.getAssignedTo())) return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /** Creates a task directly in the backlog (no sprint). */
    private void openCreateBacklogTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("New Backlog Task");
        dialog.setHeaderText("Create a new task in the backlog");

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.setPrefWidth(440);

        Label titleLabel = new Label("Title *");
        titleLabel.getStyleClass().add("dialog-label");
        TextField titleField = new TextField();
        titleField.getStyleClass().add("dialog-field");
        titleField.setPromptText("Enter task title (3-200 characters)");
        titleField.setPrefWidth(300);
        Label titleError = new Label();
        titleError.getStyleClass().add("validation-error");
        titleError.setVisible(false);
        titleError.setManaged(false);

        Label priorityLabel = new Label("Priority *");
        priorityLabel.getStyleClass().add("dialog-label");
        ComboBox<String> priorityCombo = new ComboBox<>(
            FXCollections.observableArrayList("CRITICAL", "HIGH", "MEDIUM", "LOW")
        );
        priorityCombo.getStyleClass().add("dialog-combo");
        priorityCombo.setPromptText("Select priority");
        priorityCombo.setPrefWidth(300);
        Label priorityError = new Label();
        priorityError.getStyleClass().add("validation-error");
        priorityError.setVisible(false);
        priorityError.setManaged(false);

        Label assigneeLabel = new Label("Assignee");
        assigneeLabel.getStyleClass().add("dialog-label");
        ComboBox<User> assigneeCombo = new ComboBox<>();
        assigneeCombo.getStyleClass().add("dialog-combo");
        assigneeCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "Unassigned" : u.getName(); }
            @Override public User fromString(String s) { return null; }
        });
        List<User> assigneeOpts = new ArrayList<>();
        assigneeOpts.add(null);
        assigneeOpts.addAll(teamMembers);
        assigneeCombo.setItems(FXCollections.observableArrayList(assigneeOpts));
        assigneeCombo.getSelectionModel().selectFirst();
        assigneeCombo.setPrefWidth(300);

        Label hoursLabel = new Label("Estimated Hours");
        hoursLabel.getStyleClass().add("dialog-label");
        TextField hoursField = new TextField();
        hoursField.getStyleClass().add("dialog-field");
        hoursField.setPromptText("e.g. 4.5");
        hoursField.setPrefWidth(300);
        Label hoursError = new Label();
        hoursError.getStyleClass().add("validation-error");
        hoursError.setVisible(false);
        hoursError.setManaged(false);

        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("dialog-label");
        TextArea descArea = new TextArea();
        descArea.getStyleClass().add("dialog-textarea");
        descArea.setPromptText("Describe the task (max 500 characters)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setPrefWidth(300);
        Label descError = new Label();
        descError.getStyleClass().add("validation-error");
        descError.setVisible(false);
        descError.setManaged(false);

        int fRow = 0;
        grid.add(titleLabel, 0, fRow);    grid.add(titleField, 1, fRow);     fRow++;
        grid.add(new Label(), 0, fRow);   grid.add(titleError, 1, fRow);      fRow++;
        grid.add(priorityLabel, 0, fRow); grid.add(priorityCombo, 1, fRow);  fRow++;
        grid.add(new Label(), 0, fRow);   grid.add(priorityError, 1, fRow);   fRow++;
        grid.add(assigneeLabel, 0, fRow); grid.add(assigneeCombo, 1, fRow);  fRow++;
        grid.add(hoursLabel, 0, fRow + 1);  grid.add(hoursField, 1, fRow + 1);    fRow += 2;
        grid.add(new Label(), 0, fRow);   grid.add(hoursError, 1, fRow);      fRow++;
        grid.add(descLabel, 0, fRow);     grid.add(descArea, 1, fRow);       fRow++;
        grid.add(new Label(), 0, fRow);   grid.add(descError, 1, fRow);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/project-board.css").toExternalForm()
        );
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/app-shell.css").toExternalForm()
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createType) {
                boolean valid = true;
                titleError.setVisible(false); titleError.setManaged(false);
                priorityError.setVisible(false); priorityError.setManaged(false);
                hoursError.setVisible(false); hoursError.setManaged(false);
                descError.setVisible(false); descError.setManaged(false);

                String titleVal = titleField.getText() != null ? titleField.getText().trim() : "";
                if (titleVal.isEmpty()) {
                    titleError.setText("Title is required"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                } else if (titleVal.length() < 3) {
                    titleError.setText("Title must be at least 3 characters"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                } else if (titleVal.length() > 200) {
                    titleError.setText("Title must be at most 200 characters"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                }

                String priorityVal = priorityCombo.getSelectionModel().getSelectedItem();
                if (priorityVal == null) {
                    priorityError.setText("Priority is required"); priorityError.setVisible(true); priorityError.setManaged(true); valid = false;
                }

                String hoursText = hoursField.getText() != null ? hoursField.getText().trim() : "";
                Float hoursVal = null;
                if (!hoursText.isEmpty()) {
                    try {
                        hoursVal = Float.parseFloat(hoursText);
                        if (hoursVal <= 0) {
                            hoursError.setText("Hours must be a positive number"); hoursError.setVisible(true); hoursError.setManaged(true); valid = false;
                        }
                    } catch (NumberFormatException ex) {
                        hoursError.setText("Please enter a valid number"); hoursError.setVisible(true); hoursError.setManaged(true); valid = false;
                    }
                }

                String descVal = descArea.getText() != null ? descArea.getText().trim() : "";
                if (descVal.length() > 500) {
                    descError.setText("Description must be at most 500 characters"); descError.setVisible(true); descError.setManaged(true); valid = false;
                }

                if (!valid) return null;

                Task newTask = new Task();
                newTask.setProjectId(PROJECT_ID);
                newTask.setSprintId(null); // backlog — no sprint
                newTask.setTitle(titleVal);
                newTask.setDescription(descVal.isEmpty() ? null : descVal);
                newTask.setColumnName("TODO");
                newTask.setStatus("TODO");
                newTask.setPriority(priorityVal);
                newTask.setEstimatedHours(hoursVal);

                User selectedAssignee = assigneeCombo.getSelectionModel().getSelectedItem();
                newTask.setAssignedTo(selectedAssignee != null ? selectedAssignee.getUserId() : null);

                return newTask;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(newTask -> {
            try {
                taskService.create(newTask);
                backlogTasks.add(0, newTask);
                refreshBacklogList();
            } catch (SQLException e) {
                showError("Failed to create task: " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //   CREATE SPRINT DIALOG
    // ══════════════════════════════════════════════════════

    @FXML
    private void onCreateSprint(ActionEvent event) {
        openCreateSprintDialog();
    }

    private void openCreateSprintDialog() {
        Dialog<Sprint> dialog = new Dialog<>();
        dialog.setTitle("New Sprint");
        dialog.setHeaderText("Create a new sprint");

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.setPrefWidth(460);

        // Sprint number (auto)
        int nextNumber;
        try {
            nextNumber = sprintService.getNextSprintNumber(PROJECT_ID);
        } catch (SQLException e) {
            nextNumber = sprintSelector.getItems().size() + 1;
        }

        Label numberLabel = new Label("Sprint #");
        numberLabel.getStyleClass().add("dialog-label");
        Label numberValue = new Label(String.valueOf(nextNumber));
        numberValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -spark-ink;");
        final int sprintNumber = nextNumber;

        // Title
        Label titleLabel = new Label("Title *");
        titleLabel.getStyleClass().add("dialog-label");
        TextField titleField = new TextField();
        titleField.getStyleClass().add("dialog-field");
        titleField.setPromptText("e.g. Authentication Layer");
        titleField.setPrefWidth(300);
        Label titleError = new Label();
        titleError.getStyleClass().add("validation-error");
        titleError.setVisible(false);
        titleError.setManaged(false);

        // Start Date
        Label startLabel = new Label("Start Date *");
        startLabel.getStyleClass().add("dialog-label");
        DatePicker startPicker = new DatePicker();
        startPicker.getStyleClass().add("dialog-combo");
        startPicker.setPrefWidth(300);
        startPicker.setPromptText("Select start date");
        Label startError = new Label();
        startError.getStyleClass().add("validation-error");
        startError.setVisible(false);
        startError.setManaged(false);

        // End Date
        Label endLabel = new Label("End Date *");
        endLabel.getStyleClass().add("dialog-label");
        DatePicker endPicker = new DatePicker();
        endPicker.getStyleClass().add("dialog-combo");
        endPicker.setPrefWidth(300);
        endPicker.setPromptText("Select end date");
        Label endError = new Label();
        endError.getStyleClass().add("validation-error");
        endError.setVisible(false);
        endError.setManaged(false);

        // Goal
        Label goalLabel = new Label("Goal");
        goalLabel.getStyleClass().add("dialog-label");
        TextArea goalArea = new TextArea();
        goalArea.getStyleClass().add("dialog-textarea");
        goalArea.setPromptText("Sprint goal (max 500 characters)");
        goalArea.setPrefRowCount(3);
        goalArea.setWrapText(true);
        goalArea.setPrefWidth(300);
        Label goalError = new Label();
        goalError.getStyleClass().add("validation-error");
        goalError.setVisible(false);
        goalError.setManaged(false);

        int sRow = 0;
        grid.add(numberLabel, 0, sRow);  grid.add(numberValue, 1, sRow);    sRow++;
        grid.add(titleLabel, 0, sRow);   grid.add(titleField, 1, sRow);     sRow++;
        grid.add(new Label(), 0, sRow);  grid.add(titleError, 1, sRow);      sRow++;
        grid.add(startLabel, 0, sRow);   grid.add(startPicker, 1, sRow);    sRow++;
        grid.add(new Label(), 0, sRow);  grid.add(startError, 1, sRow);      sRow++;
        grid.add(endLabel, 0, sRow);     grid.add(endPicker, 1, sRow);      sRow++;
        grid.add(new Label(), 0, sRow);  grid.add(endError, 1, sRow);        sRow++;
        grid.add(goalLabel, 0, sRow);    grid.add(goalArea, 1, sRow);       sRow++;
        grid.add(new Label(), 0, sRow);  grid.add(goalError, 1, sRow);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/project-board.css").toExternalForm()
        );
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/app-shell.css").toExternalForm()
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createType) {
                boolean valid = true;
                titleError.setVisible(false); titleError.setManaged(false);
                startError.setVisible(false); startError.setManaged(false);
                endError.setVisible(false); endError.setManaged(false);
                goalError.setVisible(false); goalError.setManaged(false);

                // Title validation
                String titleVal = titleField.getText() != null ? titleField.getText().trim() : "";
                if (titleVal.isEmpty()) {
                    titleError.setText("Title is required"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                } else if (titleVal.length() < 3) {
                    titleError.setText("Title must be at least 3 characters"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                } else if (titleVal.length() > 200) {
                    titleError.setText("Title must be at most 200 characters"); titleError.setVisible(true); titleError.setManaged(true); valid = false;
                }

                // Start date
                LocalDate startDate = startPicker.getValue();
                if (startDate == null) {
                    startError.setText("Start date is required"); startError.setVisible(true); startError.setManaged(true); valid = false;
                }

                // End date
                LocalDate endDate = endPicker.getValue();
                if (endDate == null) {
                    endError.setText("End date is required"); endError.setVisible(true); endError.setManaged(true); valid = false;
                } else if (startDate != null && !endDate.isAfter(startDate)) {
                    endError.setText("End date must be after start date"); endError.setVisible(true); endError.setManaged(true); valid = false;
                }

                // Goal
                String goalVal = goalArea.getText() != null ? goalArea.getText().trim() : "";
                if (goalVal.length() > 500) {
                    goalError.setText("Goal must be at most 500 characters"); goalError.setVisible(true); goalError.setManaged(true); valid = false;
                }

                if (!valid) return null;

                Sprint sprint = new Sprint();
                sprint.setProjectId(PROJECT_ID);
                sprint.setSprintNumber(sprintNumber);
                sprint.setTitle(titleVal);
                sprint.setStartDate(java.sql.Date.valueOf(startDate));
                sprint.setEndDate(java.sql.Date.valueOf(endDate));
                sprint.setGoal(goalVal.isEmpty() ? null : goalVal);
                sprint.setStatus("PLANNED");
                return sprint;
            }
            return null;
        });

        Optional<Sprint> result = dialog.showAndWait();
        result.ifPresent(newSprint -> {
            try {
                sprintService.create(newSprint);
                // Reload sprints and select the new one
                loadSprints();
                sprintSelector.getSelectionModel().select(newSprint);
            } catch (SQLException e) {
                showError("Failed to create sprint: " + e.getMessage());
            }
        });
    }

    // ──── Filtering ────
    @FXML
    private void onFilterChanged(ActionEvent event) {
        renderActiveTab();
    }

    /** Routes rendering to the active tab. */
    private void renderActiveTab() {
        if (activeTab == tabBoard) renderBoard();
        else if (activeTab == tabBacklog) refreshBacklogList();
    }

    private List<Task> getFilteredTasks() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String priorityVal = priorityFilter.getSelectionModel().getSelectedItem();
        String assigneeVal = assigneeFilter.getSelectionModel().getSelectedItem();

        // Resolve assignee name to user ID
        Integer assigneeId = null;
        if (assigneeVal != null && !"All".equals(assigneeVal)) {
            assigneeId = teamMembers.stream()
                .filter(u -> u.getName().equals(assigneeVal))
                .map(User::getUserId)
                .findFirst().orElse(null);
        }
        final Integer filteredAssigneeId = assigneeId;

        return allTasks.stream()
            .filter(t -> {
                if (!searchText.isEmpty()) {
                    boolean matchTitle = t.getTitle() != null && t.getTitle().toLowerCase().contains(searchText);
                    boolean matchId = ("TASK-" + t.getTaskId()).toLowerCase().contains(searchText);
                    if (!matchTitle && !matchId) return false;
                }
                if (priorityVal != null && !"All".equals(priorityVal)) {
                    if (!priorityVal.equals(t.getPriority())) return false;
                }
                if (filteredAssigneeId != null) {
                    if (t.getAssignedTo() == null || !filteredAssigneeId.equals(t.getAssignedTo())) return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════
    //   KANBAN BOARD RENDERING
    // ══════════════════════════════════════════════════════

    private void renderBoard() {
        kanbanColumns.getChildren().clear();
        List<Task> filtered = getFilteredTasks();

        // Update stats
        long done = filtered.stream().filter(t -> "DONE".equals(t.getColumnName())).count();
        statsLabel.setText(done + " / " + filtered.size() + " tasks done");

        for (String col : COLUMNS) {
            List<Task> colTasks = filtered.stream()
                .filter(t -> col.equals(t.getColumnName()))
                .collect(Collectors.toList());

            VBox column = buildColumn(col, colTasks);
            kanbanColumns.getChildren().add(column);
        }
    }

    private VBox buildColumn(String columnKey, List<Task> tasks) {
        VBox column = new VBox();
        column.getStyleClass().add("kanban-column");

        // ─── Header ───
        HBox header = new HBox(8);
        header.getStyleClass().add("column-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(COLUMN_LABELS.getOrDefault(columnKey, columnKey));
        titleLabel.getStyleClass().add("column-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        Label countLabel = new Label(String.valueOf(tasks.size()));
        countLabel.getStyleClass().add("column-count");

        header.getChildren().addAll(titleLabel, countLabel);

        // ─── Cards ───
        VBox cardsContainer = new VBox();
        cardsContainer.getStyleClass().add("column-cards");
        cardsContainer.setMinHeight(60);
        javafx.scene.layout.VBox.setVgrow(cardsContainer, javafx.scene.layout.Priority.ALWAYS);

        for (Task task : tasks) {
            VBox card = buildTaskCard(task);
            cardsContainer.getChildren().add(card);
        }

        // ─── Drop target: accept tasks dragged from other columns ───
        cardsContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != cardsContainer && event.getDragboard().hasContent(TASK_ID_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        cardsContainer.setOnDragEntered(event -> {
            if (event.getDragboard().hasContent(TASK_ID_FORMAT)) {
                cardsContainer.getStyleClass().add("column-drag-over");
            }
            event.consume();
        });
        cardsContainer.setOnDragExited(event -> {
            cardsContainer.getStyleClass().remove("column-drag-over");
            event.consume();
        });
        cardsContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(TASK_ID_FORMAT)) {
                int taskId = (int) db.getContent(TASK_ID_FORMAT);
                try {
                    taskService.updateStatus(taskId, columnKey, columnKey);
                    // Update the in-memory task
                    allTasks.stream()
                        .filter(t -> t.getTaskId() == taskId)
                        .findFirst()
                        .ifPresent(t -> {
                            t.setColumnName(columnKey);
                            t.setStatus(columnKey);
                        });
                    success = true;
                } catch (SQLException ex) {
                    showError("Failed to move task: " + ex.getMessage());
                }
            }
            event.setDropCompleted(success);
            event.consume();
            if (success) renderBoard();
        });

        ScrollPane cardsScroll = new ScrollPane(cardsContainer);
        cardsScroll.getStyleClass().add("column-cards-scroll");
        cardsScroll.setFitToWidth(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(cardsScroll, Priority.ALWAYS);

        // ─── Add Task button ───
        Button addBtn = new Button("Add task");
        addBtn.getStyleClass().add("column-add-btn");

        Region addIcon = new Region();
        addIcon.getStyleClass().add("add-icon");
        addBtn.setGraphic(addIcon);
        addBtn.setContentDisplay(ContentDisplay.LEFT);

        addBtn.setOnAction(e -> openCreateTaskDialog(columnKey));

        column.getChildren().addAll(header, cardsScroll, addBtn);
        return column;
    }

    // ══════════════════════════════════════════════════════
    //   TASK CARD
    // ══════════════════════════════════════════════════════

    private VBox buildTaskCard(Task task) {
        VBox card = new VBox(6);
        card.getStyleClass().add("task-card");

        // ─── Label chips row ───
        HBox labelsRow = new HBox(4);
        labelsRow.setAlignment(Pos.CENTER_LEFT);
        // Derive a label chip from priority (or description keywords)
        Label chipLabel = buildLabelChip(task);
        if (chipLabel != null) {
            labelsRow.getChildren().add(chipLabel);
        }

        // ─── Title ───
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("task-card-title");
        title.setWrapText(true);

        // ─── Bottom row: task ID + priority dot | story points + avatar ───
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Left side: ID + priority
        HBox leftSide = new HBox(6);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        Label idLabel = new Label("TASK-" + task.getTaskId());
        idLabel.getStyleClass().add("task-card-id");

        Region priorityDot = new Region();
        priorityDot.getStyleClass().addAll("priority-dot", getPriorityClass(task.getPriority()));

        leftSide.getChildren().addAll(idLabel, priorityDot);

        // Right side: hours + avatar
        HBox rightSide = new HBox(6);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        if (task.getEstimatedHours() != null) {
            Label pointsBadge = new Label(String.valueOf(task.getEstimatedHours().intValue()));
            pointsBadge.getStyleClass().add("story-points-badge");
            rightSide.getChildren().add(pointsBadge);
        }

        if (task.getAssignedTo() != null) {
            User assignee = teamMembers.stream()
                .filter(u -> u.getUserId() == task.getAssignedTo())
                .findFirst().orElse(null);
            String name = assignee != null ? assignee.getName() : "?";
            int idx = teamMembers.indexOf(assignee);
            String color = AVATAR_COLORS[Math.max(0, idx) % AVATAR_COLORS.length];
            StackPane avatar = buildAvatar(name, 24, color);
            rightSide.getChildren().add(avatar);
        }

        bottomRow.getChildren().addAll(leftSide, rightSide);

        if (!labelsRow.getChildren().isEmpty()) {
            card.getChildren().add(labelsRow);
        }
        card.getChildren().addAll(title, bottomRow);

        // ─── Drag: start drag on mouse press ───
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(TASK_ID_FORMAT, task.getTaskId());
            db.setContent(content);
            card.getStyleClass().add("task-card-dragging");
            event.consume();
        });
        card.setOnDragDone(event -> {
            card.getStyleClass().remove("task-card-dragging");
            event.consume();
        });

        // Click to open detail panel
        card.setOnMouseClicked(e -> openDetailPanel(task));

        return card;
    }

    private Label buildLabelChip(Task task) {
        // Map priority to a label chip color
        if (task.getPriority() == null) return null;

        String text;
        String bgColor;
        String textColor;

        switch (task.getPriority()) {
            case "CRITICAL":
                text = "Critical";
                bgColor = "#FEE2E2"; textColor = "#991B1B";
                break;
            case "HIGH":
                text = "High Priority";
                bgColor = "#FEF3C7"; textColor = "#92400E";
                break;
            case "MEDIUM":
                text = "Medium";
                bgColor = "#DBEAFE"; textColor = "#1E40AF";
                break;
            case "LOW":
                text = "Low";
                bgColor = "#DCFCE7"; textColor = "#166534";
                break;
            default:
                return null;
        }

        Label chip = new Label(text);
        chip.getStyleClass().add("task-label-chip");
        chip.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";");
        return chip;
    }

    private String getPriorityClass(String priority) {
        if (priority == null) return "priority-low";
        switch (priority) {
            case "CRITICAL": return "priority-critical";
            case "HIGH":     return "priority-high";
            case "MEDIUM":   return "priority-medium";
            case "LOW":      return "priority-low";
            default:         return "priority-low";
        }
    }

    // ══════════════════════════════════════════════════════
    //   TASK DETAIL PANEL (slide-in overlay)
    // ══════════════════════════════════════════════════════

    private void openDetailPanel(Task task) {
        detailOverlay.getChildren().clear();
        detailOverlay.setVisible(true);
        detailOverlay.setManaged(true);

        // Semi-transparent click-away area
        Region backdrop = new Region();
        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.08);");
        HBox.setHgrow(backdrop, Priority.ALWAYS);
        backdrop.setOnMouseClicked(e -> closeDetailPanel());

        // Detail panel
        VBox panel = buildDetailPanelContent(task);
        panel.getStyleClass().add("detail-panel");

        detailOverlay.getChildren().addAll(backdrop, panel);
    }

    private void closeDetailPanel() {
        detailOverlay.setVisible(false);
        detailOverlay.setManaged(false);
        detailOverlay.getChildren().clear();
    }

    private VBox buildDetailPanelContent(Task task) {
        VBox panel = new VBox();
        panel.setMinWidth(480);
        panel.setPrefWidth(480);
        panel.setMaxWidth(480);

        // ─── Header ───
        HBox header = new HBox(8);
        header.getStyleClass().add("detail-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox headerText = new VBox(2);
        Label taskIdLabel = new Label("TASK-" + task.getTaskId());
        taskIdLabel.getStyleClass().add("detail-task-id");
        Label taskTitleHeader = new Label(task.getTitle());
        taskTitleHeader.getStyleClass().add("detail-task-title-header");
        headerText.getChildren().addAll(taskIdLabel, taskTitleHeader);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("detail-close-btn");
        closeBtn.setOnAction(e -> closeDetailPanel());

        header.getChildren().addAll(headerText, closeBtn);

        // ─── Scrollable body ───
        VBox body = new VBox(16);
        body.getStyleClass().add("detail-body");

        // Validation error labels map
        Map<String, Label> errorLabels = new HashMap<>();

        // Title field
        VBox titleGroup = new VBox(2);
        Label titleHdr = new Label("TITLE");
        titleHdr.getStyleClass().add("detail-field-label");
        TextField titleField = new TextField(task.getTitle());
        titleField.getStyleClass().add("detail-title-field");
        titleField.setPromptText("Enter task title...");
        Label titleError = new Label();
        titleError.getStyleClass().add("validation-error");
        titleError.setVisible(false);
        titleError.setManaged(false);
        errorLabels.put("title", titleError);
        titleGroup.getChildren().addAll(titleHdr, titleField, titleError);

        // Status combo
        VBox statusGroup = new VBox(2);
        Label statusHdr = new Label("STATUS");
        statusHdr.getStyleClass().add("detail-field-label");
        ComboBox<String> statusCombo = new ComboBox<>(
            FXCollections.observableArrayList("TODO", "IN_PROGRESS", "REVIEW", "DONE")
        );
        statusCombo.getStyleClass().add("detail-select");
        statusCombo.getSelectionModel().select(task.getColumnName());
        statusGroup.getChildren().addAll(statusHdr, statusCombo);

        // Priority combo
        VBox priorityGroup = new VBox(2);
        Label priorityHdr = new Label("PRIORITY");
        priorityHdr.getStyleClass().add("detail-field-label");
        ComboBox<String> priorityCombo = new ComboBox<>(
            FXCollections.observableArrayList("CRITICAL", "HIGH", "MEDIUM", "LOW")
        );
        priorityCombo.getStyleClass().add("detail-select");
        priorityCombo.getSelectionModel().select(task.getPriority());
        Label priorityError = new Label();
        priorityError.getStyleClass().add("validation-error");
        priorityError.setVisible(false);
        priorityError.setManaged(false);
        errorLabels.put("priority", priorityError);
        priorityGroup.getChildren().addAll(priorityHdr, priorityCombo, priorityError);

        // Two-column row: Status + Priority
        HBox row1 = new HBox(16, statusGroup, priorityGroup);
        HBox.setHgrow(statusGroup, Priority.ALWAYS);
        HBox.setHgrow(priorityGroup, Priority.ALWAYS);

        // Assignee combo
        VBox assigneeGroup = new VBox(2);
        Label assigneeHdr = new Label("ASSIGNEE");
        assigneeHdr.getStyleClass().add("detail-field-label");
        ComboBox<User> assigneeCombo = new ComboBox<>();
        assigneeCombo.getStyleClass().add("detail-select");
        assigneeCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "Unassigned" : u.getName(); }
            @Override public User fromString(String s) { return null; }
        });
        // Add a null option for "Unassigned"
        List<User> assigneeOptions = new ArrayList<>();
        assigneeOptions.add(null); // Unassigned
        assigneeOptions.addAll(teamMembers);
        assigneeCombo.setItems(FXCollections.observableArrayList(assigneeOptions));
        // Select current
        if (task.getAssignedTo() != null) {
            User current = teamMembers.stream()
                .filter(u -> u.getUserId() == task.getAssignedTo())
                .findFirst().orElse(null);
            assigneeCombo.getSelectionModel().select(current);
        } else {
            assigneeCombo.getSelectionModel().selectFirst(); // Unassigned
        }
        assigneeGroup.getChildren().addAll(assigneeHdr, assigneeCombo);

        // Estimated hours
        VBox hoursGroup = new VBox(2);
        Label hoursHdr = new Label("ESTIMATED HOURS");
        hoursHdr.getStyleClass().add("detail-field-label");
        TextField hoursField = new TextField(
            task.getEstimatedHours() != null ? String.valueOf(task.getEstimatedHours()) : ""
        );
        hoursField.getStyleClass().add("detail-hours-field");
        hoursField.setPromptText("e.g. 4.5");
        Label hoursError = new Label();
        hoursError.getStyleClass().add("validation-error");
        hoursError.setVisible(false);
        hoursError.setManaged(false);
        errorLabels.put("hours", hoursError);
        hoursGroup.getChildren().addAll(hoursHdr, hoursField, hoursError);

        // Two-column row: Assignee + Hours
        HBox row2 = new HBox(16, assigneeGroup, hoursGroup);
        HBox.setHgrow(assigneeGroup, Priority.ALWAYS);
        HBox.setHgrow(hoursGroup, Priority.ALWAYS);

        // Description
        VBox descGroup = new VBox(2);
        Label descHdr = new Label("DESCRIPTION");
        descHdr.getStyleClass().add("detail-field-label");
        TextArea descArea = new TextArea(task.getDescription() != null ? task.getDescription() : "");
        descArea.getStyleClass().add("detail-textarea");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        Label descError = new Label();
        descError.getStyleClass().add("validation-error");
        descError.setVisible(false);
        descError.setManaged(false);
        errorLabels.put("desc", descError);
        descGroup.getChildren().addAll(descHdr, descArea, descError);

        // ─── Action buttons ───
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("detail-save-btn");

        Button deleteBtn = new Button("Delete Task");
        deleteBtn.getStyleClass().add("detail-delete-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(saveBtn, spacer, deleteBtn);

        // ─── Save handler (with validation) ───
        saveBtn.setOnAction(e -> {
            clearErrors(errorLabels);
            boolean valid = true;

            // Title validation: required, 3-200 chars
            String titleVal = titleField.getText() != null ? titleField.getText().trim() : "";
            if (titleVal.isEmpty()) {
                showFieldError(errorLabels, "title", "Title is required");
                titleField.getStyleClass().add("field-error");
                valid = false;
            } else if (titleVal.length() < 3) {
                showFieldError(errorLabels, "title", "Title must be at least 3 characters");
                titleField.getStyleClass().add("field-error");
                valid = false;
            } else if (titleVal.length() > 200) {
                showFieldError(errorLabels, "title", "Title must be at most 200 characters");
                titleField.getStyleClass().add("field-error");
                valid = false;
            }

            // Priority validation: required
            String priorityVal = priorityCombo.getSelectionModel().getSelectedItem();
            if (priorityVal == null) {
                showFieldError(errorLabels, "priority", "Priority is required");
                valid = false;
            }

            // Hours validation: positive if provided
            String hoursText = hoursField.getText() != null ? hoursField.getText().trim() : "";
            Float hoursVal = null;
            if (!hoursText.isEmpty()) {
                try {
                    hoursVal = Float.parseFloat(hoursText);
                    if (hoursVal <= 0) {
                        showFieldError(errorLabels, "hours", "Hours must be a positive number");
                        hoursField.getStyleClass().add("field-error");
                        valid = false;
                    }
                } catch (NumberFormatException ex) {
                    showFieldError(errorLabels, "hours", "Please enter a valid number");
                    hoursField.getStyleClass().add("field-error");
                    valid = false;
                }
            }

            // Description validation: max 500 chars
            String descVal = descArea.getText() != null ? descArea.getText().trim() : "";
            if (descVal.length() > 500) {
                showFieldError(errorLabels, "desc", "Description must be at most 500 characters");
                valid = false;
            }

            if (!valid) return;

            // Apply changes
            task.setTitle(titleVal);
            task.setColumnName(statusCombo.getSelectionModel().getSelectedItem());
            task.setStatus(statusCombo.getSelectionModel().getSelectedItem());
            task.setPriority(priorityVal);

            User selectedAssignee = assigneeCombo.getSelectionModel().getSelectedItem();
            task.setAssignedTo(selectedAssignee != null ? selectedAssignee.getUserId() : null);

            task.setEstimatedHours(hoursVal);
            task.setDescription(descVal.isEmpty() ? null : descVal);

            try {
                taskService.update(task);
                closeDetailPanel();
                renderBoard();
            } catch (SQLException ex) {
                showError("Failed to save: " + ex.getMessage());
            }
        });

        // ─── Delete handler ───
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete \"" + task.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Delete Task");
            confirm.setHeaderText("Confirm Deletion");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    try {
                        taskService.delete(task.getTaskId());
                        allTasks.remove(task);
                        closeDetailPanel();
                        renderBoard();
                    } catch (SQLException ex) {
                        showError("Failed to delete: " + ex.getMessage());
                    }
                }
            });
        });

        body.getChildren().addAll(titleGroup, row1, row2, descGroup, actions);

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(bodyScroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, bodyScroll);
        return panel;
    }

    // ══════════════════════════════════════════════════════
    //   CREATE TASK DIALOG
    // ══════════════════════════════════════════════════════

    private void openCreateTaskDialog(String columnKey) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("New Task");
        dialog.setHeaderText("Create a new task in " + COLUMN_LABELS.getOrDefault(columnKey, columnKey));

        // Buttons
        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        // Form
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.setPrefWidth(440);

        // Title
        Label titleLabel = new Label("Title *");
        titleLabel.getStyleClass().add("dialog-label");
        TextField titleField = new TextField();
        titleField.getStyleClass().add("dialog-field");
        titleField.setPromptText("Enter task title (3-200 characters)");
        titleField.setPrefWidth(300);
        Label titleError = new Label();
        titleError.getStyleClass().add("validation-error");
        titleError.setVisible(false);
        titleError.setManaged(false);

        // Priority
        Label priorityLabel = new Label("Priority *");
        priorityLabel.getStyleClass().add("dialog-label");
        ComboBox<String> priorityCombo = new ComboBox<>(
            FXCollections.observableArrayList("CRITICAL", "HIGH", "MEDIUM", "LOW")
        );
        priorityCombo.getStyleClass().add("dialog-combo");
        priorityCombo.setPromptText("Select priority");
        priorityCombo.setPrefWidth(300);
        Label priorityError = new Label();
        priorityError.getStyleClass().add("validation-error");
        priorityError.setVisible(false);
        priorityError.setManaged(false);

        // Assignee
        Label assigneeLabel = new Label("Assignee");
        assigneeLabel.getStyleClass().add("dialog-label");
        ComboBox<User> assigneeCombo = new ComboBox<>();
        assigneeCombo.getStyleClass().add("dialog-combo");
        assigneeCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "Unassigned" : u.getName(); }
            @Override public User fromString(String s) { return null; }
        });
        List<User> assigneeOpts = new ArrayList<>();
        assigneeOpts.add(null);
        assigneeOpts.addAll(teamMembers);
        assigneeCombo.setItems(FXCollections.observableArrayList(assigneeOpts));
        assigneeCombo.getSelectionModel().selectFirst();
        assigneeCombo.setPrefWidth(300);

        // Estimated hours
        Label hoursLabel = new Label("Estimated Hours");
        hoursLabel.getStyleClass().add("dialog-label");
        TextField hoursField = new TextField();
        hoursField.getStyleClass().add("dialog-field");
        hoursField.setPromptText("e.g. 4.5");
        hoursField.setPrefWidth(300);
        Label hoursError = new Label();
        hoursError.getStyleClass().add("validation-error");
        hoursError.setVisible(false);
        hoursError.setManaged(false);

        // Description
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("dialog-label");
        TextArea descArea = new TextArea();
        descArea.getStyleClass().add("dialog-textarea");
        descArea.setPromptText("Describe the task (max 500 characters)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setPrefWidth(300);
        Label descError = new Label();
        descError.getStyleClass().add("validation-error");
        descError.setVisible(false);
        descError.setManaged(false);

        int row = 0;
        grid.add(titleLabel, 0, row);     grid.add(titleField, 1, row);      row++;
        grid.add(new Label(), 0, row);     grid.add(titleError, 1, row);       row++;
        grid.add(priorityLabel, 0, row);  grid.add(priorityCombo, 1, row);   row++;
        grid.add(new Label(), 0, row);     grid.add(priorityError, 1, row);    row++;
        grid.add(assigneeLabel, 0, row);  grid.add(assigneeCombo, 1, row);   row++;
        grid.add(hoursLabel, 0, row + 1);    grid.add(hoursField, 1, row + 1);     row += 2;
        grid.add(new Label(), 0, row);     grid.add(hoursError, 1, row);       row++;
        grid.add(descLabel, 0, row);      grid.add(descArea, 1, row);        row++;
        grid.add(new Label(), 0, row);     grid.add(descError, 1, row);

        dialog.getDialogPane().setContent(grid);

        // Apply project-board CSS to the dialog
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/project-board.css").toExternalForm()
        );
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/app-shell.css").toExternalForm()
        );

        // Validation on Create click
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createType) {
                boolean valid = true;

                // Reset errors
                titleError.setVisible(false); titleError.setManaged(false);
                priorityError.setVisible(false); priorityError.setManaged(false);
                hoursError.setVisible(false); hoursError.setManaged(false);
                descError.setVisible(false); descError.setManaged(false);

                String titleVal = titleField.getText() != null ? titleField.getText().trim() : "";
                if (titleVal.isEmpty()) {
                    titleError.setText("Title is required");
                    titleError.setVisible(true); titleError.setManaged(true);
                    valid = false;
                } else if (titleVal.length() < 3) {
                    titleError.setText("Title must be at least 3 characters");
                    titleError.setVisible(true); titleError.setManaged(true);
                    valid = false;
                } else if (titleVal.length() > 200) {
                    titleError.setText("Title must be at most 200 characters");
                    titleError.setVisible(true); titleError.setManaged(true);
                    valid = false;
                }

                String priorityVal = priorityCombo.getSelectionModel().getSelectedItem();
                if (priorityVal == null) {
                    priorityError.setText("Priority is required");
                    priorityError.setVisible(true); priorityError.setManaged(true);
                    valid = false;
                }

                String hoursText = hoursField.getText() != null ? hoursField.getText().trim() : "";
                Float hoursVal = null;
                if (!hoursText.isEmpty()) {
                    try {
                        hoursVal = Float.parseFloat(hoursText);
                        if (hoursVal <= 0) {
                            hoursError.setText("Hours must be a positive number");
                            hoursError.setVisible(true); hoursError.setManaged(true);
                            valid = false;
                        }
                    } catch (NumberFormatException ex) {
                        hoursError.setText("Please enter a valid number");
                        hoursError.setVisible(true); hoursError.setManaged(true);
                        valid = false;
                    }
                }

                String descVal = descArea.getText() != null ? descArea.getText().trim() : "";
                if (descVal.length() > 500) {
                    descError.setText("Description must be at most 500 characters");
                    descError.setVisible(true); descError.setManaged(true);
                    valid = false;
                }

                if (!valid) return null;

                // Build the Task
                Task newTask = new Task();
                newTask.setProjectId(PROJECT_ID);
                Sprint currentSprint = sprintSelector.getSelectionModel().getSelectedItem();
                newTask.setSprintId(currentSprint != null ? currentSprint.getSprintId() : null);
                newTask.setTitle(titleVal);
                newTask.setDescription(descVal.isEmpty() ? null : descVal);
                newTask.setColumnName(columnKey);
                newTask.setStatus(columnKey);
                newTask.setPriority(priorityVal);
                newTask.setEstimatedHours(hoursVal);

                User selectedAssignee = assigneeCombo.getSelectionModel().getSelectedItem();
                newTask.setAssignedTo(selectedAssignee != null ? selectedAssignee.getUserId() : null);

                return newTask;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(newTask -> {
            try {
                taskService.create(newTask);
                allTasks.add(newTask);
                renderBoard();
            } catch (SQLException e) {
                showError("Failed to create task: " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //   HELPERS
    // ══════════════════════════════════════════════════════

    private StackPane buildAvatar(String name, double size, String bgColor) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(size >= 28 ? "team-avatar" : "card-avatar");
        avatar.setStyle("-fx-background-color: " + bgColor + ";");

        String initials = getInitials(name);
        Label text = new Label(initials);
        text.getStyleClass().add(size >= 28 ? "team-avatar-text" : "card-avatar-text");
        avatar.getChildren().add(text);
        return avatar;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void clearErrors(Map<String, Label> errorLabels) {
        for (Label l : errorLabels.values()) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    private void showFieldError(Map<String, Label> errorLabels, String key, String message) {
        Label l = errorLabels.get(key);
        if (l != null) {
            l.setText(message);
            l.setVisible(true);
            l.setManaged(true);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
