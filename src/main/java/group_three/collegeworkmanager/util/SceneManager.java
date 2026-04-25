package group_three.collegeworkmanager.util;

import group_three.collegeworkmanager.model.Role;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchToLogin() throws IOException {
        loadScene("login-view.fxml", 400, 500);
    }

    public static void switchToSignup() throws IOException {
        loadScene("signup-view.fxml", 400, 550);
    }

    public static void switchToDashboard(Role role) throws IOException {
        switch (role) {
            case ADMIN   -> loadScene("admin-dashboard.fxml", 950, 650);
            case STUDENT -> loadScene("student-dashboard.fxml", 850, 650);
            case FACULTY -> loadScene("faculty-dashboard.fxml", 950, 650);
        }
    }

    private static void loadScene(String fxml, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/group_three/collegeworkmanager/" + fxml));
        Scene scene = new Scene(loader.load(), width, height);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}
