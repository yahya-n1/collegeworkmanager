package group_three.collegeworkmanager.model;

public class Submission {
    private String id;
    private String assignmentId;
    private String studentId;
    private String studentName;
    private String content;
    private String fileUrl;
    private String submittedAt;

    public Submission(String id, String assignmentId, String studentId, String studentName,
                      String content, String fileUrl, String submittedAt) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.studentName = studentName != null ? studentName : "";
        this.content = content != null ? content : "";
        this.fileUrl = fileUrl != null ? fileUrl : "";
        this.submittedAt = submittedAt != null ? submittedAt : "";
    }

    public String getId() { return id; }
    public String getAssignmentId() { return assignmentId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getContent() { return content; }
    public String getFileUrl() { return fileUrl; }
    public String getSubmittedAt() { return submittedAt; }
}
