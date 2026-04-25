package group_three.collegeworkmanager.controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import group_three.collegeworkmanager.model.Course;
import group_three.collegeworkmanager.model.Role;
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

public class AdminDashboardController implements Initializable {

    @FXML private Label adminNameLabel;

    // User Management tab
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userRoleCol;

    // Courses tab
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> courseNameCol;
    @FXML private TableColumn<Course, String> courseCodeCol;
    @FXML private TableColumn<Course, String> courseFacultyCol;

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));
        userEmailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        userRoleCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRole() != null ? d.getValue().getRole().name() : "Pending"));

        courseNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        courseCodeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCode()));
        courseFacultyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFacultyId()));

        userTable.setItems(users);
        courseTable.setItems(courses);

        User me = AuthService.getCurrentUser();
        if (me != null) adminNameLabel.setText("Admin: " + me.getDisplayName());

        loadUsers();
        loadCourses();
    }

    @FXML
    private void handleAssignRole() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("No Selection", "Select a user first."); return; }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                selected.getRole() != null ? selected.getRole().name() : "STUDENT",
                "STUDENT", "FACULTY", "ADMIN");
        dialog.setTitle("Assign Role");
        dialog.setHeaderText("Assign role to: " + selected.getDisplayName());
        dialog.setContentText("Select role:");

        dialog.showAndWait().ifPresent(role -> new Thread(() -> {
            try {
                FirebaseService.getFirestore()
                        .collection("users").document(selected.getUid())
                        .update("role", role).get();
                Platform.runLater(() -> {
                    selected.setRole(Role.valueOf(role));
                    userTable.refresh();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to assign role: " + e.getMessage()));
            }
        }).start());
    }

    @FXML
    private void handleEditName() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("No Selection", "Select a user first."); return; }

        TextInputDialog dialog = new TextInputDialog(selected.getDisplayName());
        dialog.setTitle("Edit Name");
        dialog.setHeaderText("Editing: " + selected.getEmail());
        dialog.setContentText("New display name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            new Thread(() -> {
                try {
                    FirebaseService.getFirestore()
                            .collection("users").document(selected.getUid())
                            .update("displayName", name.trim()).get();
                    Platform.runLater(() -> {
                        selected.setDisplayName(name.trim());
                        userTable.refresh();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to update name: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("No Selection", "Select a user first."); return; }

        User me = AuthService.getCurrentUser();
        if (me != null && me.getUid().equals(selected.getUid())) {
            showError("You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getDisplayName() + "? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Delete User");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) new Thread(() -> {
                try {
                    FirebaseService.getAuth().deleteUser(selected.getUid());
                    FirebaseService.getFirestore()
                            .collection("users").document(selected.getUid()).delete().get();
                    Platform.runLater(() -> users.remove(selected));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to delete: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleCreateCourse() {
        // Build dialog
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Create Course");
        dialog.setHeaderText("Add a new course");

        TextField nameField = new TextField();
        nameField.setPromptText("Course name");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g. CSC325");

        // Populate faculty ComboBox from current user list
        ComboBox<User> facultyCombo = new ComboBox<>();
        facultyCombo.getItems().addAll(
                users.stream().filter(u -> u.getRole() == Role.FACULTY).collect(Collectors.toList()));
        facultyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getDisplayName() + " (" + u.getEmail() + ")");
            }
        });
        facultyCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getDisplayName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Code:"), codeField);
        grid.addRow(2, new Label("Faculty:"), facultyCombo);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(420);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Map<String, String> r = new HashMap<>();
            r.put("name", nameField.getText().trim());
            r.put("code", codeField.getText().trim());
            r.put("facultyId", facultyCombo.getValue() != null ? facultyCombo.getValue().getUid() : "");
            return r;
        });

        dialog.showAndWait().ifPresent(data -> {
            if (data.get("name").isEmpty() || data.get("code").isEmpty()) {
                showError("Name and code are required.");
                return;
            }
            new Thread(() -> {
                try {
                    Map<String, Object> courseData = new HashMap<>();
                    courseData.put("name", data.get("name"));
                    courseData.put("code", data.get("code"));
                    courseData.put("facultyId", data.get("facultyId"));
                    courseData.put("students", new ArrayList<>());
                    FirebaseService.getFirestore().collection("courses").add(courseData).get();
                    Platform.runLater(this::loadCourses);
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to create course: " + e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleManageStudents() {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("No Selection", "Select a course first."); return; }

        // Build dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Students — " + selected.getCode());
        dialog.setHeaderText("Add or remove students from this course");

        ObservableList<String> enrolled = FXCollections.observableArrayList(selected.getStudents());
        ListView<String> enrolledList = new ListView<>(enrolled);
        enrolledList.setPrefHeight(150);

        ComboBox<User> studentCombo = new ComboBox<>();
        studentCombo.getItems().addAll(
                users.stream().filter(u -> u.getRole() == Role.STUDENT).collect(Collectors.toList()));
        studentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getDisplayName() + " (" + u.getEmail() + ")");
            }
        });
        studentCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getDisplayName());
            }
        });

        Button addBtn = new Button("Add Student");
        addBtn.setOnAction(e -> {
            User s = studentCombo.getValue();
            if (s != null && !enrolled.contains(s.getUid())) {
                enrolled.add(s.getUid());
            }
        });

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            String sel = enrolledList.getSelectionModel().getSelectedItem();
            if (sel != null) enrolled.remove(sel);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Enrolled student UIDs:"));
        grid.add(enrolledList, 0, 1, 2, 1);
        grid.addRow(2, studentCombo, addBtn);
        grid.add(removeBtn, 0, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(v -> new Thread(() -> {
            try {
                FirebaseService.getFirestore()
                        .collection("courses").document(selected.getId())
                        .update("students", new ArrayList<>(enrolled)).get();
                Platform.runLater(() -> selected.setStudents(new ArrayList<>(enrolled)));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to update students: " + e.getMessage()));
            }
        }).start());
    }

    @FXML
    private void loadUsers() {
        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("users").get().get().getDocuments();
                List<User> loaded = docs.stream().map(doc -> {
                    String roleStr = doc.getString("role");
                    Role role = (roleStr != null && !roleStr.isEmpty()) ? Role.valueOf(roleStr) : null;
                    return new User(doc.getId(), doc.getString("email"), doc.getString("displayName"), role);
                }).collect(Collectors.toList());
                Platform.runLater(() -> users.setAll(loaded));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load users: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void loadCourses() {
        new Thread(() -> {
            try {
                List<QueryDocumentSnapshot> docs = FirebaseService.getFirestore()
                        .collection("courses").get().get().getDocuments();
                List<Course> loaded = docs.stream().map(doc -> {
                    Course c = new Course(doc.getId(), doc.getString("name"),
                            doc.getString("code"), doc.getString("facultyId"));
                    List<String> students = (List<String>) doc.get("students");
                    if (students != null) c.setStudents(students);
                    return c;
                }).collect(Collectors.toList());
                Platform.runLater(() -> courses.setAll(loaded));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load courses: " + e.getMessage()));
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
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}
