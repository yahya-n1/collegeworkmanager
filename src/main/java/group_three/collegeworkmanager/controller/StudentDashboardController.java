package group_three.collegeworkmanager.controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import group_three.collegeworkmanager.model.Assignment;
import group_three.collegeworkmanager.model.Course;
import group_three.collegeworkmanager.model.Submission;
import group_three.collegeworkmanager.model.User;
import group_three.collegeworkmanager.service.AuthService;
import group_three.collegeworkmanager.service.FirebaseService;
import group_three.collegeworkmanager.util.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class StudentDashboardController implements Initializable {

    @FXML private Label studentNameLabel;

    // Courses & Assignments tab
    @FXML private ListView<Course> courseListView;
    @FXML private Label selectedCourseLabel;
    @FXML private TableView<Assignment> assignmentTable;
    @FXML private TableColumn<Assignment, String> assignTitleCol;
    @FXML private TableColumn<Assignment, String> assignDueDateCol;
    @FXML private TableColumn<Assignment, String> assignDescCol;
    @FXML private Button submitBtn;
    @FXML private Button downloadBtn;

    // My Submissions tab
    @FXML private TableView<Submission> submissionsTable;
    @FXML private TableColumn<Submission, String> subAssignCol;
    @FXML private TableColumn<Submission, String> subDateCol;
    @FXML private TableColumn<Submission, String> subContentCol;

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private final ObservableList<Submission> submissions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User me = AuthService.getCurrentUser();
        if (me != null) studentNameLabel.setText("Student: " + me.getDisplayName());

        assignTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        assignDueDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate()));
        assignDescCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        subAssignCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAssignmentId()));
        subDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubmittedAt()));
        subContentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContent()));

        courseListView.setItems(courses);
        assignmentTable.setItems(assignments);
        submissionsTable.setItems(submissions);

        courseListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getCode() + " — " + c.getName());
            }
        });

        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, old, next) -> {
            if (next != null) {
                selectedCourseLabel.setText(next.getCode() + " — " + next.getName());
                loadAssignments(next.getId());
            }
        });

        submitBtn.setDisable(true);
        downloadBtn.setDisable(true);
        assignmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, next) -> {
            submitBtn.setDisable(next == null);
            downloadBtn.setDisable(next == null || next.getMaterialUrl().isEmpty());
        });

        loadCourses();
        loadMySubmissions();
    }

    private void loadCourses() {
        User me = AuthService.getCurrentUser();
        if (me == null) return;

        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("courses")
                        .whereArrayContains("students", me.getUid())
                        .get().get().getDocuments();
                List<Course> loaded = docs.stream()
                        .map(d -> new Course(d.getId(), d.getString("name"), d.getString("code"), d.getString("facultyId")))
                        .collect(Collectors.toList());
                Platform.runLater(() -> courses.setAll(loaded));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load courses: " + e.getMessage()));
            }
        }).start();
    }

    private void loadAssignments(String courseId) {
        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("assignments")
                        .whereEqualTo("courseId", courseId)
                        .get().get().getDocuments();
                List<Assignment> loaded = docs.stream()
                        .map(d -> new Assignment(d.getId(), d.getString("courseId"), d.getString("title"),
                                d.getString("description"), d.getString("dueDate"), d.getString("materialUrl")))
                        .collect(Collectors.toList());
                Platform.runLater(() -> assignments.setAll(loaded));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load assignments: " + e.getMessage()));
            }
        }).start();
    }

    private void loadMySubmissions() {
        User me = AuthService.getCurrentUser();
        if (me == null) return;

        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("submissions")
                        .whereEqualTo("studentId", me.getUid())
                        .get().get().getDocuments();
                List<Submission> loaded = docs.stream()
                        .map(d -> new Submission(d.getId(), d.getString("assignmentId"), d.getString("studentId"),
                                d.getString("studentName"), d.getString("content"), d.getString("fileUrl"),
                                d.getString("submittedAt")))
                        .collect(Collectors.toList());
                Platform.runLater(() -> submissions.setAll(loaded));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load submissions: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSubmitAssignment() {
        Assignment a = assignmentTable.getSelectionModel().getSelectedItem();
        User me = AuthService.getCurrentUser();
        if (a == null || me == null) return;

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Type your submission here…");
        contentArea.setPrefRowCount(8);
        contentArea.setWrapText(true);

        TextField fileUrlField = new TextField();
        fileUrlField.setPromptText("Optional: Google Drive / file URL");

        VBox box = new VBox(8,
                new Label("Assignment: " + a.getTitle()),
                new Label("Submission text:"), contentArea,
                new Label("Or file URL:"), fileUrlField);
        box.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Submit Assignment");
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setPrefWidth(460);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String text = contentArea.getText().trim();
            String url = fileUrlField.getText().trim();
            if (text.isEmpty() && url.isEmpty()) {
                showError("Provide submission text or a file URL.");
                return;
            }
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("assignmentId", a.getId());
                    data.put("studentId", me.getUid());
                    data.put("studentName", me.getDisplayName());
                    data.put("content", text);
                    data.put("fileUrl", url);
                    data.put("submittedAt", new Date().toString());
                    FirebaseService.getFirestore().collection("submissions").add(data).get();
                    Platform.runLater(() -> {
                        showInfo("Submitted!", "Your assignment was submitted successfully.");
                        loadMySubmissions();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Submit failed: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleDownloadMaterial() {
        Assignment a = assignmentTable.getSelectionModel().getSelectedItem();
        if (a == null || a.getMaterialUrl().isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(a.getMaterialUrl()));
        } catch (Exception e) {
            showError("Could not open material: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshCourses() { loadCourses(); }

    @FXML
    private void handleRefreshSubmissions() { loadMySubmissions(); }

    @FXML
    private void handleLogout() {
        AuthService.signOut();
        try { SceneManager.switchToLogin(); }
        catch (Exception e) { showError("Logout error: " + e.getMessage()); }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}
