package group_three.collegeworkmanager.model;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String id;
    private String name;
    private String code;
    private String facultyId;
    private List<String> students;

    public Course(String id, String name, String code, String facultyId) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.facultyId = facultyId;
        this.students = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getFacultyId() { return facultyId; }
    public List<String> getStudents() { return students; }

    public void setStudents(List<String> students) { this.students = students; }

    @Override
    public String toString() { return code + " - " + name; }
}
