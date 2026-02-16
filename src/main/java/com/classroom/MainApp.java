package com.classroom;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for SparkPlatform.
 * Loads the shared app-shell layout; each module plugs into the center outlet.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/app-shell.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("Spark — Academic Studio");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
