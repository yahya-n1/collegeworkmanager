package group_three.collegeworkmanager.model;

public class Assignment {
    private String id;
    private String courseId;
    private String title;
    private String description;
    private String dueDate;
    private String materialUrl;

    public Assignment(String id, String courseId, String title, String description,
                      String dueDate, String materialUrl) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.materialUrl = materialUrl;
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getDueDate() { return dueDate != null ? dueDate : ""; }
    public String getMaterialUrl() { return materialUrl != null ? materialUrl : ""; }

    @Override
    public String toString() { return title; }
}
