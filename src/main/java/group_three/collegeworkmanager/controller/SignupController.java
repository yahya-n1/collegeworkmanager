package group_three.collegeworkmanager.controller;

import group_three.collegeworkmanager.service.AuthService;
import group_three.collegeworkmanager.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button signupButton;

    @FXML
    private void handleSignup() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("All fields are required.");
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }

        signupButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                AuthService.signUp(email, password, name);
                Platform.runLater(() -> {
                    signupButton.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Account Created");
                    alert.setHeaderText("Success!");
                    alert.setContentText("Your account has been created. An administrator will assign your role before you can log in.");
                    alert.showAndWait();
                    AuthService.signOut();
                    try {
                        SceneManager.switchToLogin();
                    } catch (Exception e) {
                        errorLabel.setText("Navigation error: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    signupButton.setDisable(false);
                    errorLabel.setText(friendlyError(e.getMessage()));
                });
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            SceneManager.switchToLogin();
        } catch (Exception e) {
            errorLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    private String friendlyError(String msg) {
        if (msg == null) return "An unexpected error occurred.";
        if (msg.equals("EMAIL_EXISTS")) return "An account with this email already exists.";
        if (msg.equals("INVALID_EMAIL")) return "Invalid email address.";
        if (msg.startsWith("WEAK_PASSWORD")) return "Password must be at least 6 characters.";
        return msg;
    }
}
