package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a task in the to-do list application.
 * Demonstrates encapsulation and proper use of Java object-oriented principles.
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private String title;
    private String description;
    private String categoryId;
    private String assignedUserId;
    private TaskStatus status;
    private Priority priority;
    private LocalDateTime dueDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    /**
     * Creates a new task with a generated UUID.
     * @param title The title of the task
     * @param description The description of the task
     */
    public Task(String title, String description) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.status = TaskStatus.PENDING;
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Creates a task with all parameters (used for data loading).
     */
    public Task(String id, String title, String description, String categoryId,
                String assignedUserId, TaskStatus status, Priority priority,
                LocalDateTime dueDate, LocalDateTime createdAt,
                LocalDateTime updatedAt, LocalDateTime completedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.assignedUserId = assignedUserId;
        this.status = status;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getAssignedUserId() {
        return assignedUserId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    // Setters with automatic timestamp update
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if task is overdue.
     * @return true if past due date and not completed
     */
    public boolean isOverdue() {
        return dueDate != null && !isCompleted() && LocalDateTime.now().isAfter(dueDate);
    }

    /**
     * Sets the task status and updates timestamps appropriately.
     * @param status The new status
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        } else {
            this.completedAt = null;
        }
    }

    /**
     * Marks the task as completed.
     */
    public void markAsCompleted() {
        setStatus(TaskStatus.COMPLETED);
    }

    /**
     * Marks the task as in progress.
     */
    public void markAsInProgress() {
        setStatus(TaskStatus.IN_PROGRESS);
    }

    /**
     * Checks if the task is completed.
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Status: %s, Created: %s)",
                id.substring(0, 8), title, description, status,
                createdAt.format(FORMATTER));
    }

    /**
     * Returns a detailed string representation of the task.
     * @return Detailed task information
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task Details:\n");
        sb.append("  ID: ").append(id).append("\n");
        sb.append("  Title: ").append(title).append("\n");
        sb.append("  Description: ").append(description).append("\n");
        sb.append("  Status: ").append(status).append("\n");
        sb.append("  Category ID: ").append(categoryId != null ? categoryId : "None").append("\n");
        sb.append("  Assigned User ID: ").append(assignedUserId != null ? assignedUserId : "Unassigned").append("\n");
        sb.append("  Created: ").append(createdAt.format(FORMATTER)).append("\n");
        sb.append("  Updated: ").append(updatedAt.format(FORMATTER)).append("\n");
        if (completedAt != null) {
            sb.append("  Completed: ").append(completedAt.format(FORMATTER)).append("\n");
        }
        return sb.toString();
    }
}
