package storage;

import model.Category;
import model.Priority;
import model.Task;
import model.TaskStatus;
import model.User;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * File-based implementation of DataStore using a simple text format.
 * Demonstrates thread-safe file operations using ReentrantReadWriteLock.
 * Uses a custom format instead of JSON to avoid external dependencies.
 */
public class FileDataStore implements DataStore {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String dataDirectory;
    private final String tasksFile;
    private final String usersFile;
    private final String categoriesFile;

    // Thread-safe collections for in-memory storage
    private final ConcurrentHashMap<String, Task> tasks;
    private final ConcurrentHashMap<String, User> users;
    private final ConcurrentHashMap<String, Category> categories;

    // Read-write lock for file operations
    private final ReentrantReadWriteLock lock;

    /**
     * Creates a FileDataStore with the specified data directory.
     * @param dataDirectory The directory to store data files
     */
    public FileDataStore(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tasksFile = dataDirectory + "/tasks.dat";
        this.usersFile = dataDirectory + "/users.dat";
        this.categoriesFile = dataDirectory + "/categories.dat";

        this.tasks = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();

        this.lock = new ReentrantReadWriteLock();

        // Ensure data directory exists
        createDataDirectory();
    }

    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get(dataDirectory));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    // ==================== Task Operations ====================

    @Override
    public void saveTask(Task task) {
        tasks.put(task.getId(), task);
        persistAll();
    }

    @Override
    public Optional<Task> getTaskById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Task> getTasksByUserId(String userId) {
        return tasks.values().stream()
                .filter(task -> userId.equals(task.getAssignedUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> getTasksByCategoryId(String categoryId) {
        return tasks.values().stream()
                .filter(task -> categoryId.equals(task.getCategoryId()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTask(String id) {
        tasks.remove(id);
        persistAll();
    }

    @Override
    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
        persistAll();
    }

    // ==================== User Operations ====================

    @Override
    public void saveUser(User user) {
        users.put(user.getId(), user);
        persistAll();
    }

    @Override
    public Optional<User> getUserById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return users.values().stream()
                .filter(user -> username.equalsIgnoreCase(user.getUsername()))
                .findFirst();
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteUser(String id) {
        users.remove(id);
        persistAll();
    }

    @Override
    public void updateUser(User user) {
        users.put(user.getId(), user);
        persistAll();
    }

    // ==================== Category Operations ====================

    @Override
    public void saveCategory(Category category) {
        categories.put(category.getId(), category);
        persistAll();
    }

    @Override
    public Optional<Category> getCategoryById(String id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public Optional<Category> getCategoryByName(String name) {
        return categories.values().stream()
                .filter(cat -> name.equalsIgnoreCase(cat.getName()))
                .findFirst();
    }

    @Override
    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }

    @Override
    public void deleteCategory(String id) {
        categories.remove(id);
        persistAll();
    }

    @Override
    public void updateCategory(Category category) {
        categories.put(category.getId(), category);
        persistAll();
    }

    // ==================== Persistence Operations ====================

    @Override
    public void persistAll() {
        lock.writeLock().lock();
        try {
            persistTasks();
            persistUsers();
            persistCategories();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void loadAll() {
        lock.readLock().lock();
        try {
            loadTasks();
            loadUsers();
            loadCategories();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        tasks.clear();
        users.clear();
        categories.clear();
        persistAll();
    }

    // ==================== Private Persistence Methods ====================

    private void persistTasks() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(tasksFile))) {
            for (Task task : tasks.values()) {
                writer.println(serializeTask(task));
            }
        } catch (IOException e) {
            System.err.println("Error persisting tasks: " + e.getMessage());
        }
    }

    private void persistUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(usersFile))) {
            for (User user : users.values()) {
                writer.println(serializeUser(user));
            }
        } catch (IOException e) {
            System.err.println("Error persisting users: " + e.getMessage());
        }
    }

    private void persistCategories() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(categoriesFile))) {
            for (Category category : categories.values()) {
                writer.println(serializeCategory(category));
            }
        } catch (IOException e) {
            System.err.println("Error persisting categories: " + e.getMessage());
        }
    }

    private void loadTasks() {
        Path path = Paths.get(tasksFile);
        if (!Files.exists(path)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(tasksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Task task = deserializeTask(line);
                    if (task != null) {
                        tasks.put(task.getId(), task);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void loadUsers() {
        Path path = Paths.get(usersFile);
        if (!Files.exists(path)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    User user = deserializeUser(line);
                    if (user != null) {
                        users.put(user.getId(), user);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void loadCategories() {
        Path path = Paths.get(categoriesFile);
        if (!Files.exists(path)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(categoriesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Category category = deserializeCategory(line);
                    if (category != null) {
                        categories.put(category.getId(), category);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
    }

    // ==================== Serialization Methods ====================

    private String serializeTask(Task task) {
        // Format: id|title|description|categoryId|assignedUserId|status|priority|dueDate|createdAt|updatedAt|completedAt
        return String.join("|",
                escape(task.getId()),
                escape(task.getTitle()),
                escape(task.getDescription()),
                escape(task.getCategoryId()),
                escape(task.getAssignedUserId()),
                task.getStatus().name(),
                task.getPriority() != null ? task.getPriority().name() : "MEDIUM",
                task.getDueDate() != null ? task.getDueDate().format(FORMATTER) : "null",
                task.getCreatedAt().format(FORMATTER),
                task.getUpdatedAt().format(FORMATTER),
                task.getCompletedAt() != null ? task.getCompletedAt().format(FORMATTER) : "null"
        );
    }

    private Task deserializeTask(String line) {
        try {
            String[] parts = line.split("\\|", -1);

            // Handle both old format (9 parts) and new format (11 parts)
            if (parts.length < 9) return null;

            String id = unescape(parts[0]);
            String title = unescape(parts[1]);
            String description = unescape(parts[2]);
            String categoryId = unescape(parts[3]);
            String assignedUserId = unescape(parts[4]);
            TaskStatus status = TaskStatus.valueOf(parts[5]);

            Priority priority = Priority.MEDIUM;
            LocalDateTime dueDate = null;
            LocalDateTime createdAt;
            LocalDateTime updatedAt;
            LocalDateTime completedAt;

            if (parts.length >= 11) {
                // New format with priority and dueDate
                try {
                    priority = Priority.valueOf(parts[6]);
                } catch (IllegalArgumentException e) {
                    priority = Priority.MEDIUM;
                }
                dueDate = "null".equals(parts[7]) ? null : LocalDateTime.parse(parts[7], FORMATTER);
                createdAt = LocalDateTime.parse(parts[8], FORMATTER);
                updatedAt = LocalDateTime.parse(parts[9], FORMATTER);
                completedAt = "null".equals(parts[10]) ? null : LocalDateTime.parse(parts[10], FORMATTER);
            } else {
                // Old format without priority and dueDate
                createdAt = LocalDateTime.parse(parts[6], FORMATTER);
                updatedAt = LocalDateTime.parse(parts[7], FORMATTER);
                completedAt = "null".equals(parts[8]) ? null : LocalDateTime.parse(parts[8], FORMATTER);
            }

            return new Task(id, title, description,
                    categoryId.isEmpty() ? null : categoryId,
                    assignedUserId.isEmpty() ? null : assignedUserId,
                    status, priority, dueDate, createdAt, updatedAt, completedAt);
        } catch (Exception e) {
            System.err.println("Error deserializing task: " + e.getMessage());
            return null;
        }
    }

    private String serializeUser(User user) {
        // Format: id|username|email|createdAt|lastLoginAt|taskIds(comma-separated)
        String taskIds = String.join(",", user.getAssignedTaskIds());
        return String.join("|",
                escape(user.getId()),
                escape(user.getUsername()),
                escape(user.getEmail()),
                user.getCreatedAt().format(FORMATTER),
                user.getLastLoginAt().format(FORMATTER),
                taskIds
        );
    }

    private User deserializeUser(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 6) return null;

            String id = unescape(parts[0]);
            String username = unescape(parts[1]);
            String email = unescape(parts[2]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[3], FORMATTER);
            LocalDateTime lastLoginAt = LocalDateTime.parse(parts[4], FORMATTER);

            List<String> taskIds = new ArrayList<>();
            if (!parts[5].isEmpty()) {
                taskIds = new ArrayList<>(Arrays.asList(parts[5].split(",")));
            }

            return new User(id, username, email, createdAt, lastLoginAt, taskIds);
        } catch (Exception e) {
            System.err.println("Error deserializing user: " + e.getMessage());
            return null;
        }
    }

    private String serializeCategory(Category category) {
        // Format: id|name|description
        return String.join("|",
                escape(category.getId()),
                escape(category.getName()),
                escape(category.getDescription())
        );
    }

    private Category deserializeCategory(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 3) return null;

            return new Category(
                    unescape(parts[0]),
                    unescape(parts[1]),
                    unescape(parts[2])
            );
        } catch (Exception e) {
            System.err.println("Error deserializing category: " + e.getMessage());
            return null;
        }
    }

    // Escape pipe characters in strings
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    // Unescape pipe characters
    private String unescape(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace("\\|", "|").replace("\\\\", "\\");
    }
}
