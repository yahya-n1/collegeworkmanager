package group_three.collegeworkmanager.controller;

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
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class FacultyDashboardController implements Initializable {

    @FXML private Label facultyNameLabel;

    // My Courses tab
    @FXML private ListView<Course> courseListView;
    @FXML private Label selectedCourseLabel;
    @FXML private TableView<Assignment> assignmentTable;
    @FXML private TableColumn<Assignment, String> assignTitleCol;
    @FXML private TableColumn<Assignment, String> assignDueDateCol;
    @FXML private TableColumn<Assignment, String> assignDescCol;
    @FXML private Button editAssignBtn;
    @FXML private Button deleteAssignBtn;

    // Submissions tab
    @FXML private ComboBox<Course> submissionCourseCombo;
    @FXML private ComboBox<Assignment> submissionAssignCombo;
    @FXML private TableView<Submission> submissionsTable;
    @FXML private TableColumn<Submission, String> subStudentCol;
    @FXML private TableColumn<Submission, String> subDateCol;
    @FXML private TableColumn<Submission, String> subContentCol;
    @FXML private TableColumn<Submission, String> subFileCol;

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private final ObservableList<Submission> submissions = FXCollections.observableArrayList();
    private final ObservableList<Assignment> comboAssignments = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User me = AuthService.getCurrentUser();
        if (me != null) facultyNameLabel.setText("Faculty: " + me.getDisplayName());

        assignTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        assignDueDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate()));
        assignDescCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        subStudentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));
        subDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubmittedAt()));
        subContentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContent()));
        subFileCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileUrl()));

        courseListView.setItems(courses);
        assignmentTable.setItems(assignments);
        submissionsTable.setItems(submissions);
        submissionCourseCombo.setItems(courses);
        submissionAssignCombo.setItems(comboAssignments);

        courseListView.setCellFactory(lv -> makeCourseCell());
        submissionCourseCombo.setCellFactory(lv -> makeCourseCell());
        submissionCourseCombo.setButtonCell(makeCourseCell());

        submissionAssignCombo.setCellFactory(lv -> makeAssignCell());
        submissionAssignCombo.setButtonCell(makeAssignCell());

        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, old, next) -> {
            if (next != null) {
                selectedCourseLabel.setText(next.getCode() + " — " + next.getName());
                loadAssignments(next.getId(), false);
            }
        });

        submissionCourseCombo.valueProperty().addListener((obs, old, next) -> {
            if (next != null) loadAssignments(next.getId(), true);
        });

        editAssignBtn.setDisable(true);
        deleteAssignBtn.setDisable(true);
        assignmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, next) -> {
            editAssignBtn.setDisable(next == null);
            deleteAssignBtn.setDisable(next == null);
        });

        loadCourses();
    }

    private ListCell<Course> makeCourseCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getCode() + " — " + c.getName());
            }
        };
    }

    private ListCell<Assignment> makeAssignCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Assignment a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null : a.getTitle());
            }
        };
    }

    private void loadCourses() {
        User me = AuthService.getCurrentUser();
        if (me == null) return;

        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("courses")
                        .whereEqualTo("facultyId", me.getUid())
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

    private void loadAssignments(String courseId, boolean forCombo) {
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
                Platform.runLater(() -> {
                    if (forCombo) comboAssignments.setAll(loaded);
                    else assignments.setAll(loaded);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load assignments: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCreateAssignment() {
        Course course = courseListView.getSelectionModel().getSelectedItem();
        if (course == null) { showInfo("No Course", "Select a course first."); return; }
        showAssignmentDialog(null, course);
    }

    @FXML
    private void handleEditAssignment() {
        Assignment a = assignmentTable.getSelectionModel().getSelectedItem();
        Course course = courseListView.getSelectionModel().getSelectedItem();
        if (a == null) return;
        showAssignmentDialog(a, course);
    }

    private void showAssignmentDialog(Assignment existing, Course course) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create Assignment" : "Edit Assignment");
        dialog.setHeaderText((existing == null ? "New assignment for " : "Editing: ") +
                (course != null ? course.getName() : ""));

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Title");

        TextArea descArea = new TextArea(existing != null ? existing.getDescription() : "");
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);

        TextField dueDateField = new TextField(existing != null ? existing.getDueDate() : "");
        dueDateField.setPromptText("e.g. 2025-12-31");

        TextField materialField = new TextField(existing != null ? existing.getMaterialUrl() : "");
        materialField.setPromptText("Material URL (optional)");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Description:"), descArea);
        grid.addRow(2, new Label("Due Date:"), dueDateField);
        grid.addRow(3, new Label("Material URL:"), materialField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Map<String, String> r = new HashMap<>();
            r.put("title", titleField.getText().trim());
            r.put("description", descArea.getText().trim());
            r.put("dueDate", dueDateField.getText().trim());
            r.put("materialUrl", materialField.getText().trim());
            return r;
        });

        dialog.showAndWait().ifPresent(data -> {
            if (data.get("title").isEmpty()) { showError("Title is required."); return; }
            new Thread(() -> {
                try {
                    Map<String, Object> assignData = new HashMap<>();
                    assignData.put("courseId", course != null ? course.getId() : "");
                    assignData.put("title", data.get("title"));
                    assignData.put("description", data.get("description"));
                    assignData.put("dueDate", data.get("dueDate"));
                    assignData.put("materialUrl", data.get("materialUrl"));

                    if (existing == null) {
                        FirebaseService.getFirestore().collection("assignments").add(assignData).get();
                    } else {
                        FirebaseService.getFirestore()
                                .collection("assignments").document(existing.getId())
                                .update(assignData).get();
                    }
                    Platform.runLater(() -> { if (course != null) loadAssignments(course.getId(), false); });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to save: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleDeleteAssignment() {
        Assignment a = assignmentTable.getSelectionModel().getSelectedItem();
        Course course = courseListView.getSelectionModel().getSelectedItem();
        if (a == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + a.getTitle() + "\"? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Delete Assignment");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    FirebaseService.getFirestore()
                            .collection("assignments").document(a.getId()).delete().get();
                    Platform.runLater(() -> { if (course != null) loadAssignments(course.getId(), false); });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to delete: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleLoadSubmissions() {
        Assignment a = submissionAssignCombo.getValue();
        if (a == null) { showInfo("No Selection", "Select a course and assignment first."); return; }

        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("submissions")
                        .whereEqualTo("assignmentId", a.getId())
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
    private void handleLogout() {
        AuthService.signOut();
        try { SceneManager.switchToLogin(); }
        catch (Exception e) { showError("Logout error: " + e.getMessage()); }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.showAndWait();
    }
}
