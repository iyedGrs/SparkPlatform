package com.spark.platform.controllers;

import com.spark.platform.MainApp;
import com.spark.platform.models.User;
import com.spark.platform.services.EmailService;
import com.spark.platform.services.GoogleAuthService;
import com.spark.platform.services.GoogleAuthService.GoogleAuthResult;
import com.spark.platform.services.UserService;
import com.spark.platform.utils.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Controller for the modern login page.
 * Handles sign-in, sign-up, and forgot password flows with smooth animations.
 */
public class LoginController {

    // ═══════════════════════════════════════════════════════
    // FXML BINDINGS
    // ═══════════════════════════════════════════════════════

    // Brand Panel Elements
    @FXML private VBox brandPanel;
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;
    @FXML private Label brandTagline;

    // Form Container
    @FXML private StackPane formContainer;

    // Sign In Form
    @FXML private VBox signInForm;
    @FXML private TextField signInEmail;
    @FXML private PasswordField signInPassword;
    @FXML private Button signInButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink switchToSignUp;

    // Sign Up Form
    @FXML private VBox signUpForm;
    @FXML private ComboBox<String> signUpRoleCombo;
    @FXML private TextField signUpFirstName;
    @FXML private TextField signUpLastName;
    @FXML private TextField signUpEmail;
    @FXML private PasswordField signUpPassword;
    @FXML private PasswordField signUpConfirmPassword;
    @FXML private Button signUpButton;
    @FXML private Hyperlink switchToSignIn;

    // Forgot Password Form
    @FXML private VBox forgotPasswordForm;
    @FXML private TextField resetEmail;
    @FXML private Button resetButton;
    @FXML private Hyperlink backToSignIn;

    // Success Message
    @FXML private VBox successMessage;
    @FXML private Label successTitle;
    @FXML private Label successSubtitle;

    // Animation timelines
    private Timeline orbAnimation1;
    private Timeline orbAnimation2;
    private Timeline orbAnimation3;
    private ParallelTransition taglineAnimation;

    // Current active form
    private VBox currentForm;

    // Services
    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    // Error label (created programmatically)
    private Label signInErrorLabel;

    // Validation labels (created programmatically)
    private Label signInEmailValidation;
    private Label signUpEmailValidation;
    private Label signUpPasswordStrengthLabel;
    private Region signUpPasswordStrengthBar;
    private VBox signUpPasswordRequirements;
    private Label signUpConfirmValidation;
    private Label resetEmailValidation;

    // ═══════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        currentForm = signInForm;

        // Create error label for sign-in form
        signInErrorLabel = new Label();
        signInErrorLabel.getStyleClass().add("error-message");
        signInErrorLabel.setVisible(false);
        signInErrorLabel.setManaged(false);
        int btnIndex = signInForm.getChildren().indexOf(signInButton);
        if (btnIndex >= 0) {
            signInForm.getChildren().add(btnIndex, signInErrorLabel);
        }

        // Initialize validation components
        setupValidationComponents();

        // Initialize role selector for sign-up
        if (signUpRoleCombo != null) {
            signUpRoleCombo.getItems().addAll("Student", "Teacher");
            signUpRoleCombo.setValue("Student"); // Default selection
        }

        // Setup real-time validation listeners
        setupRealTimeValidation();

        // Start background animations
        initializeOrbAnimations();
        initializeTaglineAnimation();

        // Add input focus animations
        setupInputAnimations();

        // Add hover effects to buttons
        setupButtonEffects();

        // Entry animation
        Platform.runLater(this::playEntryAnimation);
    }

    /**
     * Creates validation labels and components for all forms.
     */
    private void setupValidationComponents() {
        // Sign-in email validation hint
        signInEmailValidation = createValidationLabel();
        insertAfterNode(signInForm, signInEmail, signInEmailValidation);

        // Sign-up email validation hint
        signUpEmailValidation = createValidationLabel();
        insertAfterNode(signUpForm, signUpEmail, signUpEmailValidation);

        // Password strength indicator for sign-up
        VBox strengthContainer = new VBox(6);
        strengthContainer.getStyleClass().add("password-strength-container");

        HBox strengthRow = new HBox(8);
        strengthRow.setAlignment(Pos.CENTER_LEFT);

        Region strengthBarBg = new Region();
        strengthBarBg.getStyleClass().add("password-strength-bar");
        strengthBarBg.setPrefWidth(120);
        strengthBarBg.setMaxWidth(120);

        signUpPasswordStrengthBar = new Region();
        signUpPasswordStrengthBar.getStyleClass().addAll("password-strength-fill");
        signUpPasswordStrengthBar.setPrefWidth(0);
        signUpPasswordStrengthBar.setMaxWidth(120);

        StackPane barStack = new StackPane(strengthBarBg, signUpPasswordStrengthBar);
        barStack.setAlignment(Pos.CENTER_LEFT);
        barStack.setMaxWidth(120);

        signUpPasswordStrengthLabel = new Label();
        signUpPasswordStrengthLabel.getStyleClass().add("password-strength-label");

        strengthRow.getChildren().addAll(barStack, signUpPasswordStrengthLabel);

        // Password requirements checklist
        signUpPasswordRequirements = new VBox(4);
        signUpPasswordRequirements.getStyleClass().add("validation-requirements");

        strengthContainer.getChildren().addAll(strengthRow, signUpPasswordRequirements);
        insertAfterNode(signUpForm, signUpPassword, strengthContainer);

        // Confirm password validation
        signUpConfirmValidation = createValidationLabel();
        insertAfterNode(signUpForm, signUpConfirmPassword, signUpConfirmValidation);

        // Reset email validation
        resetEmailValidation = createValidationLabel();
        insertAfterNode(forgotPasswordForm, resetEmail, resetEmailValidation);
    }

    /**
     * Creates a validation label with default styling.
     */
    private Label createValidationLabel() {
        Label label = new Label();
        label.getStyleClass().add("validation-message");
        label.setVisible(false);
        label.setManaged(false);
        label.setWrapText(true);
        return label;
    }

    /**
     * Inserts a node immediately after another node in a VBox.
     */
    private void insertAfterNode(VBox parent, javafx.scene.Node target, javafx.scene.Node toInsert) {
        if (parent == null || target == null) return;
        int index = parent.getChildren().indexOf(target);
        if (index >= 0 && index < parent.getChildren().size()) {
            parent.getChildren().add(index + 1, toInsert);
        }
    }

    /**
     * Sets up real-time validation listeners for all input fields.
     */
    private void setupRealTimeValidation() {
        // Sign-in email validation
        if (signInEmail != null) {
            signInEmail.textProperty().addListener((obs, oldVal, newVal) -> {
                validateEmailField(signInEmail, signInEmailValidation, newVal, false);
            });
            signInEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && !signInEmail.getText().isEmpty()) {
                    validateEmailField(signInEmail, signInEmailValidation, signInEmail.getText(), true);
                }
            });
        }

        // Sign-up email validation
        if (signUpEmail != null) {
            signUpEmail.textProperty().addListener((obs, oldVal, newVal) -> {
                validateEmailField(signUpEmail, signUpEmailValidation, newVal, false);
            });
            signUpEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && !signUpEmail.getText().isEmpty()) {
                    validateEmailField(signUpEmail, signUpEmailValidation, signUpEmail.getText(), true);
                }
            });
        }

        // Sign-up password strength
        if (signUpPassword != null) {
            signUpPassword.textProperty().addListener((obs, oldVal, newVal) -> {
                updatePasswordStrength(newVal);
                updatePasswordRequirements(newVal);
                // Also re-validate confirm password if it has content
                if (signUpConfirmPassword != null && !signUpConfirmPassword.getText().isEmpty()) {
                    validatePasswordMatch(signUpConfirmPassword.getText(), newVal);
                }
            });
        }

        // Confirm password validation
        if (signUpConfirmPassword != null) {
            signUpConfirmPassword.textProperty().addListener((obs, oldVal, newVal) -> {
                String password = signUpPassword != null ? signUpPassword.getText() : "";
                validatePasswordMatch(newVal, password);
            });
        }

        // Reset email validation
        if (resetEmail != null) {
            resetEmail.textProperty().addListener((obs, oldVal, newVal) -> {
                validateEmailField(resetEmail, resetEmailValidation, newVal, false);
            });
            resetEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && !resetEmail.getText().isEmpty()) {
                    validateEmailField(resetEmail, resetEmailValidation, resetEmail.getText(), true);
                }
            });
        }
    }

    /**
     * Validates an email field and shows appropriate feedback.
     */
    private void validateEmailField(TextField field, Label validationLabel, String email, boolean showSuccess) {
        clearAllStates(field);
        
        if (email == null || email.isEmpty()) {
            hideValidation(validationLabel);
            return;
        }

        if (isValidEmail(email)) {
            field.getStyleClass().add("success");
            if (showSuccess) {
                showValidation(validationLabel, "✓ Valid email format", "success");
            } else {
                hideValidation(validationLabel);
            }
        } else if (email.contains("@") && email.length() > 3) {
            // Partial email - show hint
            field.getStyleClass().add("error");
            showValidation(validationLabel, "Please enter a valid email address", "error");
        } else {
            hideValidation(validationLabel);
        }
    }

    /**
     * Updates the password strength indicator.
     */
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            signUpPasswordStrengthBar.setPrefWidth(0);
            signUpPasswordStrengthLabel.setText("");
            clearStrengthStyles();
            return;
        }

        int strength = calculatePasswordStrength(password);
        String strengthText;
        String styleClass;
        double width;

        if (strength < 2) {
            strengthText = "Weak";
            styleClass = "weak";
            width = 30;
        } else if (strength < 3) {
            strengthText = "Fair";
            styleClass = "fair";
            width = 60;
        } else if (strength < 4) {
            strengthText = "Good";
            styleClass = "good";
            width = 90;
        } else {
            strengthText = "Strong";
            styleClass = "strong";
            width = 120;
        }

        // Animate width change
        Timeline widthAnim = new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(signUpPasswordStrengthBar.prefWidthProperty(), width, Interpolator.EASE_OUT)
            )
        );
        widthAnim.play();

        clearStrengthStyles();
        signUpPasswordStrengthBar.getStyleClass().add(styleClass);
        signUpPasswordStrengthLabel.getStyleClass().add(styleClass);
        signUpPasswordStrengthLabel.setText(strengthText);
    }

    /**
     * Clears all strength indicator styles.
     */
    private void clearStrengthStyles() {
        signUpPasswordStrengthBar.getStyleClass().removeAll("weak", "fair", "good", "strong");
        signUpPasswordStrengthLabel.getStyleClass().removeAll("weak", "fair", "good", "strong");
    }

    /**
     * Calculates password strength score (0-5).
     */
    private int calculatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        return score;
    }

    /**
     * Updates the password requirements checklist.
     */
    private void updatePasswordRequirements(String password) {
        signUpPasswordRequirements.getChildren().clear();
        
        if (password == null || password.isEmpty()) return;

        addRequirementItem("At least 8 characters", password.length() >= 8);
        addRequirementItem("Contains lowercase letter", password.matches(".*[a-z].*"));
        addRequirementItem("Contains uppercase letter", password.matches(".*[A-Z].*"));
        addRequirementItem("Contains a number", password.matches(".*[0-9].*"));
    }

    /**
     * Adds a requirement item to the list.
     */
    private void addRequirementItem(String text, boolean isMet) {
        HBox item = new HBox(8);
        item.getStyleClass().add("requirement-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Region icon = new Region();
        icon.getStyleClass().addAll("requirement-icon", isMet ? "met" : "pending");

        Label label = new Label(text);
        label.getStyleClass().addAll("requirement-text", isMet ? "met" : "pending");

        item.getChildren().addAll(icon, label);
        signUpPasswordRequirements.getChildren().add(item);
    }

    /**
     * Validates password confirmation matches.
     */
    private void validatePasswordMatch(String confirm, String password) {
        clearAllStates(signUpConfirmPassword);
        
        if (confirm == null || confirm.isEmpty()) {
            hideValidation(signUpConfirmValidation);
            return;
        }

        if (confirm.equals(password)) {
            signUpConfirmPassword.getStyleClass().add("success");
            showValidation(signUpConfirmValidation, "✓ Passwords match", "success");
        } else {
            signUpConfirmPassword.getStyleClass().add("error");
            showValidation(signUpConfirmValidation, "Passwords do not match", "error");
        }
    }

    /**
     * Shows a validation message with specified style.
     */
    private void showValidation(Label label, String message, String styleClass) {
        if (label == null) return;
        label.setText(message);
        label.getStyleClass().removeAll("error", "success", "hint");
        label.getStyleClass().add(styleClass);
        label.setVisible(true);
        label.setManaged(true);
    }

    /**
     * Hides a validation message.
     */
    private void hideValidation(Label label) {
        if (label == null) return;
        label.setVisible(false);
        label.setManaged(false);
    }

    /**
     * Clears all validation states from a field.
     */
    private void clearAllStates(TextField field) {
        if (field == null) return;
        field.getStyleClass().removeAll("error", "success");
    }

    /**
     * Creates floating orb animations for the brand panel background.
     * Each orb follows a unique elliptical path with different timing.
     */
    private void initializeOrbAnimations() {
        if (orb1 == null || orb2 == null || orb3 == null) return;

        // Orb 1: Large, slow floating motion
        orbAnimation1 = createOrbAnimation(orb1, -100, -150, 60, 40, 8000);
        
        // Orb 2: Medium, medium speed
        orbAnimation2 = createOrbAnimation(orb2, 120, 100, 45, 35, 6000);
        
        // Orb 3: Small, faster motion
        orbAnimation3 = createOrbAnimation(orb3, -80, 180, 30, 25, 5000);

        // Start all animations
        orbAnimation1.play();
        orbAnimation2.play();
        orbAnimation3.play();
    }

    /**
     * Creates an elliptical floating animation for an orb.
     */
    private Timeline createOrbAnimation(Circle orb, double startX, double startY,
                                         double radiusX, double radiusY, int durationMs) {
        Timeline timeline = new Timeline();
        
        // Create smooth elliptical motion using keyframes
        int steps = 60;
        for (int i = 0; i <= steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            double x = startX + radiusX * Math.cos(angle);
            double y = startY + radiusY * Math.sin(angle);
            
            KeyFrame kf = new KeyFrame(
                Duration.millis((durationMs * i) / steps),
                new KeyValue(orb.translateXProperty(), x, Interpolator.EASE_BOTH),
                new KeyValue(orb.translateYProperty(), y, Interpolator.EASE_BOTH)
            );
            timeline.getKeyFrames().add(kf);
        }
        
        timeline.setCycleCount(Animation.INDEFINITE);
        return timeline;
    }

    /**
     * Creates a subtle pulsing animation for the brand tagline.
     */
    private void initializeTaglineAnimation() {
        if (brandTagline == null) return;

        FadeTransition fade = new FadeTransition(Duration.millis(3000), brandTagline);
        fade.setFromValue(0.8);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);

        fade.play();
    }

    /**
     * Sets up focus animations for input fields.
     */
    private void setupInputAnimations() {
        // Add subtle scale animation on focus
        TextField[] textFields = {signInEmail, signUpFirstName, signUpLastName, signUpEmail, resetEmail};
        PasswordField[] passwordFields = {signInPassword, signUpPassword, signUpConfirmPassword};

        for (TextField field : textFields) {
            if (field != null) {
                addInputFocusEffect(field);
            }
        }

        for (PasswordField field : passwordFields) {
            if (field != null) {
                addInputFocusEffect(field);
            }
        }
    }

    /**
     * Adds subtle focus animation to a text input.
     */
    private void addInputFocusEffect(TextField field) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), field);
            if (isFocused) {
                scale.setToX(1.01);
                scale.setToY(1.01);
            } else {
                scale.setToX(1.0);
                scale.setToY(1.0);
            }
            scale.play();
        });
    }

    /**
     * Sets up button hover effects.
     */
    private void setupButtonEffects() {
        Button[] buttons = {signInButton, signUpButton, resetButton};
        
        for (Button btn : buttons) {
            if (btn != null) {
                // Pulse effect on hover
                btn.setOnMouseEntered(e -> {
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(200), btn);
                    pulse.setToX(1.02);
                    pulse.setToY(1.02);
                    pulse.play();
                });
                
                btn.setOnMouseExited(e -> {
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(200), btn);
                    pulse.setToX(1.0);
                    pulse.setToY(1.0);
                    pulse.play();
                });
            }
        }
    }

    /**
     * Plays the initial entry animation when the page loads.
     */
    private void playEntryAnimation() {
        // Brand panel slide in from left
        if (brandPanel != null) {
            brandPanel.setTranslateX(-50);
            brandPanel.setOpacity(0);
            
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(600), brandPanel);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), brandPanel);
            fadeIn.setToValue(1);
            
            ParallelTransition brandEntry = new ParallelTransition(slideIn, fadeIn);
            brandEntry.play();
        }

        // Form slide in from right
        if (signInForm != null) {
            signInForm.setTranslateX(30);
            signInForm.setOpacity(0);
            
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(600), signInForm);
            slideIn.setDelay(Duration.millis(200));
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), signInForm);
            fadeIn.setDelay(Duration.millis(200));
            fadeIn.setToValue(1);
            
            ParallelTransition formEntry = new ParallelTransition(slideIn, fadeIn);
            formEntry.play();
        }
    }

    // ═══════════════════════════════════════════════════════
    // FORM TRANSITION ANIMATIONS
    // ═══════════════════════════════════════════════════════

    /**
     * Smoothly transitions between authentication forms.
     */
    private void switchToForm(VBox newForm, boolean slideFromRight) {
        if (newForm == currentForm || currentForm == null) return;

        VBox oldForm = currentForm;
        currentForm = newForm;

        // Prepare new form
        newForm.setVisible(true);
        newForm.setOpacity(0);
        newForm.setTranslateX(slideFromRight ? 30 : -30);

        // Animate out old form
        TranslateTransition oldSlide = new TranslateTransition(Duration.millis(300), oldForm);
        oldSlide.setToX(slideFromRight ? -30 : 30);
        oldSlide.setInterpolator(Interpolator.EASE_IN);

        FadeTransition oldFade = new FadeTransition(Duration.millis(300), oldForm);
        oldFade.setToValue(0);

        ParallelTransition oldExit = new ParallelTransition(oldSlide, oldFade);
        oldExit.setOnFinished(e -> {
            oldForm.setVisible(false);
            oldForm.setTranslateX(0); // Reset for next use
        });

        // Animate in new form
        TranslateTransition newSlide = new TranslateTransition(Duration.millis(400), newForm);
        newSlide.setDelay(Duration.millis(150));
        newSlide.setToX(0);
        newSlide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition newFade = new FadeTransition(Duration.millis(400), newForm);
        newFade.setDelay(Duration.millis(150));
        newFade.setToValue(1);

        ParallelTransition newEntry = new ParallelTransition(newSlide, newFade);

        // Play both animations
        oldExit.play();
        newEntry.play();
    }

    /**
     * Creates a shake animation for error feedback.
     */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    /**
     * Shows an error state on an input field.
     */
    private void showInputError(TextField field) {
        clearAllStates(field);
        field.getStyleClass().add("error");
        shakeNode(field);
    }

    /**
     * Clears error state from an input field.
     */
    private void clearInputError(TextField field) {
        clearAllStates(field);
    }

    /**
     * Shows loading state on a button.
     */
    private void setButtonLoading(Button button, boolean loading) {
        button.setDisable(loading);
        
        if (loading) {
            String originalText = (String) button.getUserData();
            if (originalText == null) {
                button.setUserData(button.getText());
            }
            button.setText("Please wait...");
            button.setOpacity(0.7);
        } else {
            String originalText = (String) button.getUserData();
            if (originalText != null) {
                button.setText(originalText);
            }
            button.setOpacity(1.0);
        }
    }

    // ═══════════════════════════════════════════════════════
    // NAVIGATION HANDLERS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void showSignIn(ActionEvent event) {
        switchToForm(signInForm, false);
    }

    @FXML
    private void showSignUp(ActionEvent event) {
        switchToForm(signUpForm, true);
    }

    @FXML
    private void showForgotPassword(ActionEvent event) {
        // Pre-fill email if available
        if (signInEmail.getText() != null && !signInEmail.getText().isEmpty()) {
            resetEmail.setText(signInEmail.getText());
        }
        switchToForm(forgotPasswordForm, true);
    }

    @FXML
    private void showSuccessMessage(String title, String subtitle) {
        successTitle.setText(title);
        successSubtitle.setText(subtitle);
        
        switchToForm(successMessage, true);

        // Add bounce animation to success icon
        if (successMessage.lookup(".success-icon") != null) {
            javafx.scene.Node icon = successMessage.lookup(".success-icon");
            
            ScaleTransition bounce = new ScaleTransition(Duration.millis(300), icon);
            bounce.setDelay(Duration.millis(400));
            bounce.setFromX(0);
            bounce.setFromY(0);
            bounce.setToX(1);
            bounce.setToY(1);
            bounce.setInterpolator(Interpolator.EASE_OUT);
            
            // Add overshoot effect
            ScaleTransition overshoot = new ScaleTransition(Duration.millis(150), icon);
            overshoot.setFromX(1);
            overshoot.setFromY(1);
            overshoot.setToX(1.1);
            overshoot.setToY(1.1);
            
            ScaleTransition settle = new ScaleTransition(Duration.millis(150), icon);
            settle.setToX(1);
            settle.setToY(1);
            
            SequentialTransition bounceSequence = new SequentialTransition(bounce, overshoot, settle);
            bounceSequence.play();
        }
    }

    // ═══════════════════════════════════════════════════════
    // AUTHENTICATION HANDLERS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleSignIn(ActionEvent event) {
        // Clear previous errors
        clearInputError(signInEmail);
        clearInputError(signInPassword);
        signInErrorLabel.setVisible(false);
        signInErrorLabel.setManaged(false);

        String email = signInEmail.getText().trim();
        String password = signInPassword.getText();

        // Validate inputs
        boolean hasError = false;

        if (email.isEmpty() || !isValidEmail(email)) {
            showInputError(signInEmail);
            hasError = true;
        }

        if (password.isEmpty()) {
            showInputError(signInPassword);
            hasError = true;
        }

        if (hasError) return;

        // Show loading state
        setButtonLoading(signInButton, true);

        // Authenticate on background thread (BCrypt is deliberately slow)
        new Thread(() -> {
            try {
                User user = userService.authenticate(email, password);
                Platform.runLater(() -> {
                    setButtonLoading(signInButton, false);
                    if (user != null) {
                        // Login successful
                        SessionManager.getInstance().login(user);
                        playButtonSuccessAnimation(signInButton);

                        Timeline navigateDelay = new Timeline(new KeyFrame(Duration.millis(300), ev -> {
                            MainApp.showMainApplication();
                        }));
                        navigateDelay.play();
                    } else {
                        // Login failed
                        signInErrorLabel.setText("Invalid email or password");
                        signInErrorLabel.setVisible(true);
                        signInErrorLabel.setManaged(true);
                        shakeNode(signInButton);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setButtonLoading(signInButton, false);
                    signInErrorLabel.setText("Connection error. Please try again.");
                    signInErrorLabel.setVisible(true);
                    signInErrorLabel.setManaged(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        // Clear previous errors
        clearInputError(signUpFirstName);
        clearInputError(signUpLastName);
        clearInputError(signUpEmail);
        clearInputError(signUpPassword);
        clearInputError(signUpConfirmPassword);

        String firstName = signUpFirstName.getText().trim();
        String lastName = signUpLastName.getText().trim();
        String email = signUpEmail.getText().trim();
        String password = signUpPassword.getText();
        String confirmPassword = signUpConfirmPassword.getText();

        // Validate inputs
        boolean hasError = false;

        if (firstName.isEmpty()) {
            showInputError(signUpFirstName);
            hasError = true;
        }

        if (lastName.isEmpty()) {
            showInputError(signUpLastName);
            hasError = true;
        }

        if (email.isEmpty() || !isValidEmail(email)) {
            showInputError(signUpEmail);
            hasError = true;
        }

        if (password.isEmpty() || password.length() < 8) {
            showInputError(signUpPassword);
            hasError = true;
        }

        if (!password.equals(confirmPassword)) {
            showInputError(signUpConfirmPassword);
            hasError = true;
        }

        if (hasError) return;

        // Get selected role
        String selectedRole = signUpRoleCombo.getValue();
        String userType = "Student".equals(selectedRole) ? "STUDENT" : "TEACHER";

        // Show loading state
        setButtonLoading(signUpButton, true);

        final String fName = firstName;
        final String lName = lastName;
        final String role = userType;

        // Create account in background thread
        new Thread(() -> {
            try {
                // Check if email already exists
                User existing = userService.findByEmail(email);
                if (existing != null) {
                    Platform.runLater(() -> {
                        setButtonLoading(signUpButton, false);
                        showSignUpError("An account already exists for " + email + ".\nPlease use Sign In instead.");
                    });
                    return;
                }

                // Create user with selected role
                User newUser = new User();
                newUser.setName(fName + " " + lName);
                newUser.setEmail(email);
                newUser.setUserType(role);
                newUser.setStatus("ACTIVE");

                User createdUser = userService.create(newUser, password);

                // Send welcome email
                boolean emailSent = emailService.sendWelcomeEmail(createdUser, password);

                Platform.runLater(() -> {
                    setButtonLoading(signUpButton, false);

                    System.out.println("[SignUp] Account created for: " + email);
                    if (emailSent) {
                        showSuccessMessage("Account created!",
                                "Welcome to Spark, " + fName + "!\n\n" +
                                "A confirmation has been sent to " + email + ".");
                    } else {
                        showSuccessMessage("Account created!",
                                "Welcome to Spark, " + fName + "!\n\n" +
                                "You can now sign in with your email and password.");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setButtonLoading(signUpButton, false);
                    System.err.println("[SignUp] Failed to create account: " + ex.getMessage());
                    showSignUpError("Failed to create account. Please try again later.");
                });
            }
        }).start();
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        clearInputError(resetEmail);

        String email = resetEmail.getText().trim();

        if (email.isEmpty() || !isValidEmail(email)) {
            showInputError(resetEmail);
            return;
        }

        // Show loading state
        setButtonLoading(resetButton, true);

        // Process password reset on background thread
        new Thread(() -> {
            try {
                // Check if user exists
                User user = userService.findByEmail(email);
                
                Platform.runLater(() -> {
                    setButtonLoading(resetButton, false);
                    
                    if (user == null) {
                        // Don't reveal if email exists or not for security
                        // Show same success message regardless
                        showSuccessMessage("Check your email", 
                            "If an account exists for " + email + ", you will receive a password reset email shortly.");
                        return;
                    }
                    
                    // Generate new password and update in database
                    try {
                        String newPassword = UserService.generatePassword();
                        userService.updatePassword(user.getUserId(), newPassword);
                        
                        // Send password reset email
                        boolean emailSent = emailService.sendPasswordResetEmail(user, newPassword);
                        
                        if (emailSent) {
                            System.out.println("[Login] Password reset email sent to: " + email);
                            showSuccessMessage("Check your email", 
                                "We've sent your new password to " + email + ". Please check your inbox.");
                        } else {
                            // Email failed but password was changed - show error
                            showResetError("Password was reset but email could not be sent. Please contact an administrator.");
                        }
                        
                    } catch (Exception ex) {
                        System.err.println("[Login] Password reset failed: " + ex.getMessage());
                        showResetError("Failed to reset password. Please try again later.");
                    }
                });
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setButtonLoading(resetButton, false);
                    showResetError("Connection error. Please try again.");
                });
            }
        }).start();
    }

    /**
     * Shows an error message in the forgot password form.
     */
    private void showResetError(String message) {
        // Create or find error label in forgot password form
        Label errorLabel = (Label) forgotPasswordForm.lookup(".reset-error-label");
        if (errorLabel == null) {
            errorLabel = new Label();
            errorLabel.getStyleClass().addAll("error-message", "reset-error-label");
            errorLabel.setWrapText(true);
            // Insert before the reset button
            int btnIndex = forgotPasswordForm.getChildren().indexOf(resetButton);
            if (btnIndex >= 0) {
                forgotPasswordForm.getChildren().add(btnIndex, errorLabel);
            } else {
                forgotPasswordForm.getChildren().add(errorLabel);
            }
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        shakeNode(resetButton);
    }

    // ═══════════════════════════════════════════════════════
    // SOCIAL LOGIN HANDLERS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGoogleSignIn(ActionEvent event) {
        Button btn = (Button) event.getSource();
        animateSocialButton(btn);

        if (!googleAuthService.isAvailable()) {
            showGoogleAuthError("Google sign-in is not configured. Please contact an administrator.");
            return;
        }

        // Show loading state
        btn.setDisable(true);
        btn.setText("Connecting...");

        // Run OAuth flow in background thread (opens browser)
        new Thread(() -> {
            try {
                // Authenticate with Google - allow all user types (ADMINISTRATOR, TEACHER, STUDENT)
                GoogleAuthResult result = googleAuthService.authenticateWithGoogle(
                        "ADMINISTRATOR", "TEACHER", "STUDENT"
                );

                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText(""); // Social buttons usually just have icons

                    if (result.isSuccess()) {
                        // Login successful
                        User user = result.getUser();
                        SessionManager.getInstance().login(user);
                        System.out.println("[Login] Google sign-in successful for: " + user.getEmail());

                        playButtonSuccessAnimation(btn);

                        Timeline navigateDelay = new Timeline(new KeyFrame(Duration.millis(300), ev -> {
                            MainApp.showMainApplication();
                        }));
                        navigateDelay.play();
                    } else {
                        // Show appropriate error based on result
                        switch (result.getStatus()) {
                            case USER_NOT_FOUND:
                                showGoogleAuthError("No Spark account found for " + result.getGoogleEmail() + ".\nPlease contact an administrator to create your account.");
                                break;
                            case UNAUTHORIZED_ROLE:
                                showGoogleAuthError(result.getErrorMessage());
                                break;
                            case ACCOUNT_DISABLED:
                                showGoogleAuthError("Your account has been disabled.\nPlease contact the ESPRIT Administration.");
                                break;
                            default:
                                showGoogleAuthError(result.getErrorMessage());
                        }
                    }
                });

            } catch (GoogleAuthService.GoogleAuthException e) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("");
                    showGoogleAuthError("Google sign-in failed. Please try again.");
                });
            }
        }).start();
    }

    /**
     * Shows an error message for Google authentication failures.
     */
    private void showGoogleAuthError(String message) {
        // Hide any previous error and show the sign-in error
        signInErrorLabel.setText(message);
        signInErrorLabel.setVisible(true);
        signInErrorLabel.setManaged(true);
        shakeNode(signInButton);
    }

    @FXML
    private void handleMicrosoftSignIn(ActionEvent event) {
        animateSocialButton((Button) event.getSource());
        // TODO: Implement Microsoft OAuth
        System.out.println("Microsoft sign-in clicked");
    }

    @FXML
    private void handleGitHubSignIn(ActionEvent event) {
        animateSocialButton((Button) event.getSource());
        // TODO: Implement GitHub OAuth
        System.out.println("GitHub sign-in clicked");
    }

    // ═══════════════════════════════════════════════════════
    // SOCIAL SIGN-UP HANDLERS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGoogleSignUp(ActionEvent event) {
        Button btn = (Button) event.getSource();
        animateSocialButton(btn);

        if (!googleAuthService.isAvailable()) {
            showSignUpError("Google sign-up is not configured. Please contact an administrator.");
            return;
        }

        // Validate role selection
        String selectedRole = signUpRoleCombo.getValue();
        if (selectedRole == null || selectedRole.isEmpty()) {
            showSignUpError("Please select your role (Student or Teacher) first.");
            return;
        }

        // Show loading state
        btn.setDisable(true);

        // Run OAuth flow in background thread (opens browser)
        // Force account selection so user can choose which Google account to use
        new Thread(() -> {
            try {
                // Authenticate with Google to get user info - force account picker
                GoogleAuthResult result = googleAuthService.authenticateWithGoogle(true);

                Platform.runLater(() -> {
                    btn.setDisable(false);

                    String googleEmail = result.getGoogleEmail();
                    String googleName = result.getGoogleName();

                    if (result.isSuccess() || result.getStatus() == GoogleAuthResult.Status.UNAUTHORIZED_ROLE) {
                        // User already exists in the database
                        showSignUpError("An account already exists for " + googleEmail + ".\nPlease use Sign In instead.");
                        return;
                    }

                    if (result.getStatus() == GoogleAuthResult.Status.USER_NOT_FOUND) {
                        // Perfect - user doesn't exist yet, create a new account
                        createAccountFromGoogle(googleEmail, googleName);
                    } else {
                        showSignUpError(result.getErrorMessage());
                    }
                });

            } catch (GoogleAuthService.GoogleAuthException e) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    showSignUpError("Google sign-up failed. Please try again.");
                });
            }
        }).start();
    }

    /**
     * Creates a new account using Google profile information.
     */
    private void createAccountFromGoogle(String email, String fullName) {
        // Parse name into first and last
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.lastIndexOf(" ");
            firstName = fullName.substring(0, spaceIndex).trim();
            lastName = fullName.substring(spaceIndex + 1).trim();
        }

        // Get selected role from ComboBox
        String selectedRole = signUpRoleCombo.getValue();
        String userType = "Teacher".equals(selectedRole) ? "TEACHER" : "STUDENT";

        // Generate a random password (user can sign in with Google or use this)
        String generatedPassword = UserService.generatePassword();

        // Show loading state
        setButtonLoading(signUpButton, true);

        final String fName = firstName;
        final String lName = lastName;
        final String role = userType;

        // Create account in background thread
        new Thread(() -> {
            try {
                // Create user with selected role
                User newUser = new User();
                newUser.setName(fullName != null ? fullName : email.split("@")[0]);
                newUser.setEmail(email);
                newUser.setUserType(role);
                newUser.setStatus("ACTIVE");

                User createdUser = userService.create(newUser, generatedPassword);

                // Send welcome email with password
                boolean emailSent = emailService.sendWelcomeEmail(createdUser, generatedPassword);

                Platform.runLater(() -> {
                    setButtonLoading(signUpButton, false);

                    if (emailSent) {
                        System.out.println("[SignUp] Account created via Google for: " + email);
                        showSuccessMessage("Account created!",
                                "Welcome to Spark, " + fName + "!\n\n" +
                                "Your password has been sent to " + email + ".\n" +
                                "You can sign in with Google or use your email and password.");
                    } else {
                        // Account created but email failed
                        showSuccessMessage("Account created!",
                                "Welcome to Spark, " + fName + "!\n\n" +
                                "Note: We couldn't send your password via email.\n" +
                                "Please use Google Sign In to access your account.");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setButtonLoading(signUpButton, false);
                    System.err.println("[SignUp] Failed to create account: " + ex.getMessage());
                    showSignUpError("Failed to create account. Please try again later.");
                });
            }
        }).start();
    }

    /**
     * Shows an error message in the sign-up form.
     */
    private void showSignUpError(String message) {
        // Create or find error label in sign-up form
        Label errorLabel = (Label) signUpForm.lookup(".signup-error-label");
        if (errorLabel == null) {
            errorLabel = new Label();
            errorLabel.getStyleClass().addAll("error-message", "signup-error-label");
            errorLabel.setWrapText(true);
            // Insert before the sign-up button
            int btnIndex = signUpForm.getChildren().indexOf(signUpButton);
            if (btnIndex >= 0) {
                signUpForm.getChildren().add(btnIndex, errorLabel);
            } else {
                signUpForm.getChildren().add(errorLabel);
            }
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        shakeNode(signUpButton);
    }

    @FXML
    private void handleMicrosoftSignUp(ActionEvent event) {
        animateSocialButton((Button) event.getSource());
        // TODO: Implement Microsoft OAuth for sign-up
        showSignUpError("Microsoft sign-up coming soon!");
    }

    @FXML
    private void handleGitHubSignUp(ActionEvent event) {
        animateSocialButton((Button) event.getSource());
        // TODO: Implement GitHub OAuth for sign-up
        showSignUpError("GitHub sign-up coming soon!");
    }

    /**
     * Animates a social button press.
     */
    private void animateSocialButton(Button button) {
        ScaleTransition press = new ScaleTransition(Duration.millis(100), button);
        press.setToX(0.9);
        press.setToY(0.9);
        
        ScaleTransition release = new ScaleTransition(Duration.millis(100), button);
        release.setToX(1.0);
        release.setToY(1.0);
        
        SequentialTransition click = new SequentialTransition(press, release);
        click.play();
    }

    /**
     * Plays a success animation on a button.
     */
    private void playButtonSuccessAnimation(Button button) {
        // Quick scale pulse
        ScaleTransition pulse1 = new ScaleTransition(Duration.millis(100), button);
        pulse1.setToX(1.05);
        pulse1.setToY(1.05);
        
        ScaleTransition pulse2 = new ScaleTransition(Duration.millis(100), button);
        pulse2.setToX(1.0);
        pulse2.setToY(1.0);
        
        SequentialTransition success = new SequentialTransition(pulse1, pulse2);
        success.play();
    }

    // ═══════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════

    /**
     * Simple email validation.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Cleans up animations when the view is closed.
     */
    public void cleanup() {
        if (orbAnimation1 != null) orbAnimation1.stop();
        if (orbAnimation2 != null) orbAnimation2.stop();
        if (orbAnimation3 != null) orbAnimation3.stop();
        if (taglineAnimation != null) taglineAnimation.stop();
    }
}
