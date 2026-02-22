package api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Category;
import model.Priority;
import model.Task;
import model.TaskStatus;
import model.User;
import service.TaskManager;
import service.UserManager;
import storage.DataStore;
import storage.FileDataStore;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API Server for the Collaborative To-Do List Application.
 * Provides HTTP endpoints for the React frontend.
 */
public class ApiServer {
    private static final String DATA_DIR = "data";
    private static final int PORT = 8080;

    private final HttpServer server;
    private final DataStore dataStore;
    private final TaskManager taskManager;
    private final UserManager userManager;

    public ApiServer() throws IOException {
        this.dataStore = new FileDataStore(DATA_DIR);
        this.taskManager = new TaskManager(dataStore);
        this.userManager = new UserManager(dataStore);

        // Load existing data
        dataStore.loadAll();
        createDefaultCategories();

        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        setupEndpoints();
    }

    private void createDefaultCategories() {
        String[] defaultCategories = {"Work", "Personal", "Shopping", "Health", "Learning"};
        for (String categoryName : defaultCategories) {
            if (dataStore.getCategoryByName(categoryName).isEmpty()) {
                dataStore.saveCategory(new Category(categoryName, categoryName + " related tasks"));
            }
        }
    }

    private void setupEndpoints() {
        // CORS handler wrapper
        server.createContext("/api/tasks", this::handleTasks);
        server.createContext("/api/users", this::handleUsers);
        server.createContext("/api/categories", this::handleCategories);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(method)) {
                // Check if getting specific task or all tasks
                if (path.matches("/api/tasks/[^/]+")) {
                    String taskId = path.substring("/api/tasks/".length());
                    handleGetTask(exchange, taskId);
                } else {
                    handleGetAllTasks(exchange);
                }
            } else if ("POST".equals(method)) {
                handleCreateTask(exchange);
            } else if ("PUT".equals(method)) {
                String taskId = path.substring("/api/tasks/".length());
                handleUpdateTask(exchange, taskId);
            } else if ("DELETE".equals(method)) {
                String taskId = path.substring("/api/tasks/".length());
                handleDeleteTask(exchange, taskId);
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetAllTasks(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getAllTasks();
        String json = tasksToJson(tasks);
        sendResponse(exchange, 200, json);
    }

    private void handleGetTask(HttpExchange exchange, String taskId) throws IOException {
        Optional<Task> task = taskManager.getTask(taskId);
        if (task.isPresent()) {
            sendResponse(exchange, 200, taskToJson(task.get()));
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Task not found\"}");
        }
    }

    private void handleCreateTask(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        // Simple JSON parsing
        String title = extractJsonValue(body, "title");
        String description = extractJsonValue(body, "description");
        String categoryId = extractJsonValue(body, "categoryId");
        String assignedUserId = extractJsonValue(body, "assignedUserId");
        String priorityStr = extractJsonValue(body, "priority");

        Task task;
        if (categoryId != null || assignedUserId != null) {
            task = taskManager.createTask(title, description, categoryId, assignedUserId);
        } else {
            task = taskManager.createTask(title, description);
        }

        // Set priority if provided
        if (priorityStr != null && !priorityStr.isEmpty()) {
            try {
                Priority priority = Priority.valueOf(priorityStr.toUpperCase());
                task.setPriority(priority);
                dataStore.saveTask(task);
            } catch (IllegalArgumentException e) {
                // Invalid priority, keep default
            }
        }

        sendResponse(exchange, 201, taskToJson(task));
    }

    private void handleUpdateTask(HttpExchange exchange, String taskId) throws IOException {
        String body = readRequestBody(exchange);
        Optional<Task> taskOpt = taskManager.getTask(taskId);

        if (taskOpt.isEmpty()) {
            sendResponse(exchange, 404, "{\"error\": \"Task not found\"}");
            return;
        }

        String title = extractJsonValue(body, "title");
        String description = extractJsonValue(body, "description");
        String status = extractJsonValue(body, "status");
        String categoryId = extractJsonValue(body, "categoryId");
        String assignedUserId = extractJsonValue(body, "assignedUserId");
        String priorityStr = extractJsonValue(body, "priority");

        if (title != null) {
            taskManager.updateTaskTitle(taskId, title);
        }
        if (description != null) {
            taskManager.updateTaskDescription(taskId, description);
        }
        if (status != null) {
            TaskStatus newStatus = TaskStatus.valueOf(status);
            taskManager.setTaskStatus(taskId, newStatus);
        }
        if (categoryId != null) {
            taskManager.setTaskCategory(taskId, categoryId);
        }
        if (priorityStr != null && !priorityStr.isEmpty()) {
            try {
                Priority priority = Priority.valueOf(priorityStr.toUpperCase());
                taskOpt.get().setPriority(priority);
                dataStore.saveTask(taskOpt.get());
            } catch (IllegalArgumentException e) {
                // Invalid priority, ignore
            }
        }
        if (assignedUserId != null) {
            taskManager.assignTask(taskId, assignedUserId);
        }

        Optional<Task> updatedTask = taskManager.getTask(taskId);
        sendResponse(exchange, 200, taskToJson(updatedTask.get()));
    }

    private void handleDeleteTask(HttpExchange exchange, String taskId) throws IOException {
        boolean deleted = taskManager.deleteTask(taskId);
        if (deleted) {
            sendResponse(exchange, 200, "{\"message\": \"Task deleted\"}");
        } else {
            sendResponse(exchange, 404, "{\"error\": \"Task not found\"}");
        }
    }

    private void handleUsers(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(method)) {
                if (path.matches("/api/users/[^/]+")) {
                    String userId = path.substring("/api/users/".length());
                    handleGetUser(exchange, userId);
                } else {
                    handleGetAllUsers(exchange);
                }
            } else if ("POST".equals(method)) {
                handleCreateUser(exchange);
            } else if ("DELETE".equals(method)) {
                String userId = path.substring("/api/users/".length());
                handleDeleteUser(exchange, userId);
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetAllUsers(HttpExchange exchange) throws IOException {
        List<User> users = userManager.getAllUsers();
        String json = usersToJson(users);
        sendResponse(exchange, 200, json);
    }

    private void handleGetUser(HttpExchange exchange, String userId) throws IOException {
        Optional<User> user = userManager.getUser(userId);
        if (user.isPresent()) {
            sendResponse(exchange, 200, userToJson(user.get()));
        } else {
            sendResponse(exchange, 404, "{\"error\": \"User not found\"}");
        }
    }

    private void handleCreateUser(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        String username = extractJsonValue(body, "username");
        String email = extractJsonValue(body, "email");

        User user;
        if (email != null && !email.isEmpty()) {
            user = userManager.createUser(username, email);
        } else {
            user = userManager.createUser(username);
        }

        if (user != null) {
            sendResponse(exchange, 201, userToJson(user));
        } else {
            sendResponse(exchange, 400, "{\"error\": \"Username already exists\"}");
        }
    }

    private void handleDeleteUser(HttpExchange exchange, String userId) throws IOException {
        boolean deleted = userManager.deleteUser(userId);
        if (deleted) {
            sendResponse(exchange, 200, "{\"message\": \"User deleted\"}");
        } else {
            sendResponse(exchange, 404, "{\"error\": \"User not found\"}");
        }
    }

    private void handleCategories(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if ("GET".equals(method)) {
                List<Category> categories = dataStore.getAllCategories();
                String json = categoriesToJson(categories);
                sendResponse(exchange, 200, json);
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // JSON Helpers
    private String taskToJson(Task task) {
        return String.format(
            "{\"id\":\"%s\",\"title\":\"%s\",\"description\":\"%s\",\"categoryId\":%s,\"assignedUserId\":%s,\"status\":\"%s\",\"priority\":\"%s\",\"dueDate\":%s,\"isOverdue\":%s,\"createdAt\":\"%s\",\"updatedAt\":\"%s\",\"completedAt\":%s}",
            escapeJson(task.getId()),
            escapeJson(task.getTitle()),
            escapeJson(task.getDescription()),
            task.getCategoryId() != null ? "\"" + escapeJson(task.getCategoryId()) + "\"" : "null",
            task.getAssignedUserId() != null ? "\"" + escapeJson(task.getAssignedUserId()) + "\"" : "null",
            task.getStatus().name(),
            task.getPriority() != null ? task.getPriority().name() : "MEDIUM",
            task.getDueDate() != null ? "\"" + task.getDueDate().toString() + "\"" : "null",
            task.isOverdue(),
            task.getCreatedAt().toString(),
            task.getUpdatedAt().toString(),
            task.getCompletedAt() != null ? "\"" + task.getCompletedAt().toString() + "\"" : "null"
        );
    }

    private String tasksToJson(List<Task> tasks) {
        String items = tasks.stream()
            .map(this::taskToJson)
            .collect(Collectors.joining(","));
        return "[" + items + "]";
    }

    private String userToJson(User user) {
        return String.format(
            "{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"createdAt\":\"%s\",\"lastLoginAt\":\"%s\"}",
            escapeJson(user.getId()),
            escapeJson(user.getUsername()),
            escapeJson(user.getEmail()),
            user.getCreatedAt().toString(),
            user.getLastLoginAt().toString()
        );
    }

    private String usersToJson(List<User> users) {
        String items = users.stream()
            .map(this::userToJson)
            .collect(Collectors.joining(","));
        return "[" + items + "]";
    }

    private String categoryToJson(Category category) {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"description\":\"%s\"}",
            escapeJson(category.getId()),
            escapeJson(category.getName()),
            escapeJson(category.getDescription())
        );
    }

    private String categoriesToJson(List<Category> categories) {
        String items = categories.stream()
            .map(this::categoryToJson)
            .collect(Collectors.joining(","));
        return "[" + items + "]";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*";
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            int valueEnd = json.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else if (firstChar == 'n' && json.substring(valueStart).startsWith("null")) {
            return null;
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void start() {
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        COLLABORATIVE TO-DO LIST API SERVER                   ║");
        System.out.println("║              Running on http://localhost:" + PORT + "                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("\nAPI Endpoints:");
        System.out.println("  GET    /api/tasks          - Get all tasks");
        System.out.println("  GET    /api/tasks/{id}     - Get task by ID");
        System.out.println("  POST   /api/tasks          - Create a task");
        System.out.println("  PUT    /api/tasks/{id}     - Update a task");
        System.out.println("  DELETE /api/tasks/{id}     - Delete a task");
        System.out.println("  GET    /api/users          - Get all users");
        System.out.println("  GET    /api/users/{id}     - Get user by ID");
        System.out.println("  POST   /api/users          - Create a user");
        System.out.println("  GET    /api/categories     - Get all categories");
        System.out.println("\nPress Ctrl+C to stop the server...");
    }

    public void stop() {
        server.stop(0);
    }

    public static void main(String[] args) {
        try {
            ApiServer apiServer = new ApiServer();
            apiServer.start();

            // Keep running until interrupted
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                apiServer.stop();
            }));

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
