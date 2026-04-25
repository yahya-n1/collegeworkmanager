package group_three.collegeworkmanager.model;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private Role role;

    public User(String uid, String email, String displayName, Role role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() { return displayName + " (" + email + ")"; }
}
