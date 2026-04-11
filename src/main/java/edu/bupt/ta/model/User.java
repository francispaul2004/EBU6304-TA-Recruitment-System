package edu.bupt.ta.model;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.repository.Identifiable;

public class User implements Identifiable<String> {
    private String userId;
    private String username;
    private String passwordHash;
    private Role role;
    private String displayName;
    private boolean active;

    public User() {
    }

    public User(String userId, String username, String passwordHash, Role role, String displayName, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.active = active;
    }

    @Override
    public String getId() {
        return userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
