package com.spark.platform;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.services.UserService;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX entry point for SparkPlatform.
 * Starts with the login page, then transitions to the app-shell after authentication.
 */
public class MainApp extends Application {

    private static Stage primaryStage;
    private static final double WINDOW_WIDTH = 1280;
    private static final double WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize database (triggers MySQL attempt, H2 fallback)
        DatabaseConfig.getInstance();

        // Ensure default admin account exists (idempotent)
        try {
            new UserService().ensureAdminExists();
        } catch (Exception e) {
            System.err.println("[Auth] Warning: Could not ensure admin exists: " + e.getMessage());
        }

        // Start with the login page
        showLoginPage();

        stage.setTitle("Spark \u2014 Academic Studio");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
    }

    /**
     * Displays the login page.
     */
    public static void showLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/fxml/login-view.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Smooth transition if scene already exists
            if (primaryStage.getScene() != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), primaryStage.getScene().getRoot());
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    primaryStage.setScene(scene);
                    playFadeIn(root);
                });
                fadeOut.play();
            } else {
                primaryStage.setScene(scene);
                playFadeIn(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load login page: " + e.getMessage());
        }
    }

    /**
     * Displays the main application shell after successful authentication.
     */
    public static void showMainApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/fxml/app-shell.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Smooth transition from login to main app
            if (primaryStage.getScene() != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), primaryStage.getScene().getRoot());
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    primaryStage.setScene(scene);
                    playFadeIn(root);
                });
                fadeOut.play();
            } else {
                primaryStage.setScene(scene);
                playFadeIn(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load main application: " + e.getMessage());
        }
    }

    /**
     * Plays a fade-in animation on a node.
     */
    private static void playFadeIn(Parent root) {
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Returns the primary stage for external access.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
