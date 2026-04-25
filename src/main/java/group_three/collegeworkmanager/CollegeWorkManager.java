package group_three.collegeworkmanager;

import group_three.collegeworkmanager.service.FirebaseService;
import group_three.collegeworkmanager.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class CollegeWorkManager extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FirebaseService.initialize();
        } catch (RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Firebase Configuration Error");
            alert.setHeaderText("Could not connect to Firebase");
            alert.setContentText(
                "1. Place serviceAccountKey.json in the project root directory.\n" +
                "2. Set your Web API Key in FirebaseConfig.java.\n\n" + e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        SceneManager.initialize(stage);
        try {
            SceneManager.switchToLogin();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load login screen", e);
        }
        stage.setTitle("College Work Manager");
        stage.show();
    }
}
