package group_three.collegeworkmanager.controller;

import group_three.collegeworkmanager.service.AuthService;
import group_three.collegeworkmanager.model.User;
import group_three.collegeworkmanager.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password are required.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                User user = AuthService.signIn(email, password);
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (user.getRole() == null) {
                        AuthService.signOut();
                        errorLabel.setText("Your account is pending role assignment. Contact an administrator.");
                    } else {
                        try {
                            SceneManager.switchToDashboard(user.getRole());
                        } catch (Exception e) {
                            errorLabel.setText("Error loading dashboard: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    errorLabel.setText(friendlyError(e.getMessage()));
                });
            }
        }).start();
    }

    @FXML
    private void goToSignup() {
        try {
            SceneManager.switchToSignup();
        } catch (Exception e) {
            errorLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    private String friendlyError(String msg) {
        if (msg == null) return "An unexpected error occurred.";
        return switch (msg) {
            case "INVALID_LOGIN_CREDENTIALS"       -> "Invalid email or password.";
            case "EMAIL_NOT_FOUND"                 -> "No account found with this email.";
            case "INVALID_PASSWORD"                -> "Incorrect password.";
            case "USER_DISABLED"                   -> "This account has been disabled.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER"     -> "Too many attempts. Please try again later.";
            default -> msg;
        };
    }
}
