package com.spark.platform.controllers;

import com.spark.platform.MainApp;
import com.spark.platform.models.User;
import com.spark.platform.services.UserService;
import com.spark.platform.utils.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    // Error label (created programmatically)
    private Label signInErrorLabel;

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

        // Hide sign-up link (only admin creates accounts)
        if (switchToSignUp != null && switchToSignUp.getParent() != null) {
            switchToSignUp.getParent().setVisible(false);
            ((javafx.scene.Node) switchToSignUp.getParent()).setManaged(false);
        }

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
        if (!field.getStyleClass().contains("error")) {
            field.getStyleClass().add("error");
        }
        shakeNode(field);
    }

    /**
     * Clears error state from an input field.
     */
    private void clearInputError(TextField field) {
        field.getStyleClass().remove("error");
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

        // Show loading state
        setButtonLoading(signUpButton, true);

        // Simulate account creation (replace with actual logic)
        Timeline createDelay = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
            setButtonLoading(signUpButton, false);
            
            // TODO: Implement actual account creation
            System.out.println("Account creation attempted for: " + email);
            
            // Show success message
            showSuccessMessage("Account created!", 
                "Welcome to Spark, " + firstName + "! Please check your email to verify your account.");
        }));
        createDelay.play();
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

        // Simulate password reset (replace with actual logic)
        Timeline resetDelay = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            setButtonLoading(resetButton, false);
            
            // TODO: Implement actual password reset
            System.out.println("Password reset requested for: " + email);
            
            // Show success message
            showSuccessMessage("Check your email", 
                "We've sent a password reset link to " + email);
        }));
        resetDelay.play();
    }

    // ═══════════════════════════════════════════════════════
    // SOCIAL LOGIN HANDLERS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGoogleSignIn(ActionEvent event) {
        animateSocialButton((Button) event.getSource());
        // TODO: Implement Google OAuth
        System.out.println("Google sign-in clicked");
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
