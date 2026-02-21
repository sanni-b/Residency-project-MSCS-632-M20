package service;

import model.Category;
import model.Task;
import model.TaskStatus;
import storage.DataStore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Service class for managing tasks with thread-safe operations.
 * Demonstrates the use of ReentrantReadWriteLock for concurrent access.
 */
public class TaskManager {
    private final DataStore dataStore;
    private final ReentrantReadWriteLock lock;

    /**
     * Creates a TaskManager with the specified data store.
     * @param dataStore The data store for persistence
     */
    public TaskManager(DataStore dataStore) {
        this.dataStore = dataStore;
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Creates a new task.
     * @param title The task title
     * @param description The task description
     * @return The created task
     */
    public Task createTask(String title, String description) {
        lock.writeLock().lock();
        try {
            Task task = new Task(title, description);
            dataStore.saveTask(task);
            System.out.println("[TaskManager] Task created: " + task.getId().substring(0, 8));
            return task;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a task with category and user assignment.
     * @param title The task title
     * @param description The task description
     * @param categoryId The category ID
     * @param userId The assigned user ID
     * @return The created task
     */
    public Task createTask(String title, String description, String categoryId, String userId) {
        lock.writeLock().lock();
        try {
            Task task = new Task(title, description);
            task.setCategoryId(categoryId);
            task.setAssignedUserId(userId);
            dataStore.saveTask(task);
            System.out.println("[TaskManager] Task created with assignment: " + task.getId().substring(0, 8));
            return task;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a task by ID.
     * @param taskId The task ID
     * @return Optional containing the task if found
     */
    public Optional<Task> getTask(String taskId) {
        lock.readLock().lock();
        try {
            return dataStore.getTaskById(taskId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all tasks.
     * @return List of all tasks
     */
    public List<Task> getAllTasks() {
        lock.readLock().lock();
        try {
            return dataStore.getAllTasks();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets tasks assigned to a specific user.
     * @param userId The user ID
     * @return List of tasks assigned to the user
     */
    public List<Task> getTasksByUser(String userId) {
        lock.readLock().lock();
        try {
            return dataStore.getTasksByUserId(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets tasks in a specific category.
     * @param categoryId The category ID
     * @return List of tasks in the category
     */
    public List<Task> getTasksByCategory(String categoryId) {
        lock.readLock().lock();
        try {
            return dataStore.getTasksByCategoryId(categoryId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets tasks by status.
     * @param status The task status
     * @return List of tasks with the specified status
     */
    public List<Task> getTasksByStatus(TaskStatus status) {
        lock.readLock().lock();
        try {
            return dataStore.getAllTasks().stream()
                    .filter(task -> task.getStatus() == status)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates a task's title.
     * @param taskId The task ID
     * @param newTitle The new title
     * @return true if successful, false otherwise
     */
    public boolean updateTaskTitle(String taskId, String newTitle) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setTitle(newTitle);
                dataStore.updateTask(task);
                System.out.println("[TaskManager] Task title updated: " + taskId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates a task's description.
     * @param taskId The task ID
     * @param newDescription The new description
     * @return true if successful, false otherwise
     */
    public boolean updateTaskDescription(String taskId, String newDescription) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setDescription(newDescription);
                dataStore.updateTask(task);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Marks a task as completed.
     * @param taskId The task ID
     * @return true if successful, false otherwise
     */
    public boolean markTaskComplete(String taskId) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.markAsCompleted();
                dataStore.updateTask(task);
                System.out.println("[TaskManager] Task marked complete: " + taskId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Marks a task as in progress.
     * @param taskId The task ID
     * @return true if successful, false otherwise
     */
    public boolean markTaskInProgress(String taskId) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.markAsInProgress();
                dataStore.updateTask(task);
                System.out.println("[TaskManager] Task marked in progress: " + taskId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets task status.
     * @param taskId The task ID
     * @param status The new status
     * @return true if successful, false otherwise
     */
    public boolean setTaskStatus(String taskId, TaskStatus status) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setStatus(status);
                dataStore.updateTask(task);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Assigns a task to a user.
     * @param taskId The task ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    public boolean assignTask(String taskId, String userId) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setAssignedUserId(userId);
                dataStore.updateTask(task);
                System.out.println("[TaskManager] Task " + taskId.substring(0, 8) +
                        " assigned to user " + userId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets task category.
     * @param taskId The task ID
     * @param categoryId The category ID
     * @return true if successful, false otherwise
     */
    public boolean setTaskCategory(String taskId, String categoryId) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setCategoryId(categoryId);
                dataStore.updateTask(task);
                System.out.println("[TaskManager] Task " + taskId.substring(0, 8) +
                        " categorized");
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a task.
     * @param taskId The task ID
     * @return true if successful, false otherwise
     */
    public boolean deleteTask(String taskId) {
        lock.writeLock().lock();
        try {
            Optional<Task> taskOpt = dataStore.getTaskById(taskId);
            if (taskOpt.isPresent()) {
                dataStore.deleteTask(taskId);
                System.out.println("[TaskManager] Task deleted: " + taskId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets task count.
     * @return Total number of tasks
     */
    public int getTaskCount() {
        lock.readLock().lock();
        try {
            return dataStore.getAllTasks().size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets count of completed tasks.
     * @return Number of completed tasks
     */
    public int getCompletedTaskCount() {
        lock.readLock().lock();
        try {
            return (int) dataStore.getAllTasks().stream()
                    .filter(Task::isCompleted)
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets count of pending tasks.
     * @return Number of pending tasks
     */
    public int getPendingTaskCount() {
        lock.readLock().lock();
        try {
            return (int) dataStore.getAllTasks().stream()
                    .filter(task -> task.getStatus() == TaskStatus.PENDING)
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Category Operations ====================

    /**
     * Creates a new category.
     * @param name The category name
     * @return The created category
     */
    public Category createCategory(String name) {
        lock.writeLock().lock();
        try {
            Category category = new Category(name);
            dataStore.saveCategory(category);
            System.out.println("[TaskManager] Category created: " + name);
            return category;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a category with description.
     * @param name The category name
     * @param description The category description
     * @return The created category
     */
    public Category createCategory(String name, String description) {
        lock.writeLock().lock();
        try {
            Category category = new Category(name, description);
            dataStore.saveCategory(category);
            System.out.println("[TaskManager] Category created: " + name);
            return category;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a category by ID.
     * @param categoryId The category ID
     * @return Optional containing the category if found
     */
    public Optional<Category> getCategory(String categoryId) {
        lock.readLock().lock();
        try {
            return dataStore.getCategoryById(categoryId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a category by name.
     * @param name The category name
     * @return Optional containing the category if found
     */
    public Optional<Category> getCategoryByName(String name) {
        lock.readLock().lock();
        try {
            return dataStore.getCategoryByName(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all categories.
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        lock.readLock().lock();
        try {
            return dataStore.getAllCategories();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Deletes a category.
     * @param categoryId The category ID
     * @return true if successful, false otherwise
     */
    public boolean deleteCategory(String categoryId) {
        lock.writeLock().lock();
        try {
            Optional<Category> catOpt = dataStore.getCategoryById(categoryId);
            if (catOpt.isPresent()) {
                dataStore.deleteCategory(categoryId);
                System.out.println("[TaskManager] Category deleted: " + categoryId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
