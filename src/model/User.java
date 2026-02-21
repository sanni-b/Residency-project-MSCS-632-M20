package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user in the collaborative to-do list application.
 * Demonstrates encapsulation and proper use of collections.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private String username;
    private String email;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private final List<String> assignedTaskIds;

    /**
     * Creates a new user with a generated UUID.
     * @param username The username
     */
    public User(String username) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = "";
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
        this.assignedTaskIds = new ArrayList<>();
    }

    /**
     * Creates a new user with username and email.
     * @param username The username
     * @param email The user's email
     */
    public User(String username, String email) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
        this.assignedTaskIds = new ArrayList<>();
    }

    /**
     * Creates a user with all parameters (used for data loading).
     */
    public User(String id, String username, String email, LocalDateTime createdAt,
                LocalDateTime lastLoginAt, List<String> assignedTaskIds) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.assignedTaskIds = new ArrayList<>(assignedTaskIds);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * Returns an unmodifiable view of assigned task IDs.
     * @return List of assigned task IDs
     */
    public List<String> getAssignedTaskIds() {
        return Collections.unmodifiableList(assignedTaskIds);
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Updates the last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Assigns a task to this user.
     * @param taskId The task ID to assign
     */
    public void assignTask(String taskId) {
        if (!assignedTaskIds.contains(taskId)) {
            assignedTaskIds.add(taskId);
        }
    }

    /**
     * Removes a task assignment from this user.
     * @param taskId The task ID to remove
     */
    public void unassignTask(String taskId) {
        assignedTaskIds.remove(taskId);
    }

    /**
     * Checks if a task is assigned to this user.
     * @param taskId The task ID to check
     * @return true if assigned, false otherwise
     */
    public boolean hasTask(String taskId) {
        return assignedTaskIds.contains(taskId);
    }

    /**
     * Gets the number of assigned tasks.
     * @return Number of assigned tasks
     */
    public int getTaskCount() {
        return assignedTaskIds.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', email='%s', tasks=%d}",
                id.substring(0, 8), username, email, assignedTaskIds.size());
    }

    /**
     * Returns a detailed string representation of the user.
     * @return Detailed user information
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User Details:\n");
        sb.append("  ID: ").append(id).append("\n");
        sb.append("  Username: ").append(username).append("\n");
        sb.append("  Email: ").append(email.isEmpty() ? "Not set" : email).append("\n");
        sb.append("  Created: ").append(createdAt.format(FORMATTER)).append("\n");
        sb.append("  Last Login: ").append(lastLoginAt.format(FORMATTER)).append("\n");
        sb.append("  Assigned Tasks: ").append(assignedTaskIds.size()).append("\n");
        return sb.toString();
    }
}
