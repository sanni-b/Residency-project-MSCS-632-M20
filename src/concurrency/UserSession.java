package concurrency;

import model.Category;
import model.Task;
import model.TaskStatus;
import model.User;
import service.TaskManager;
import service.UserManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/**
 * Represents a user session running in its own thread.
 * Demonstrates Java threading for concurrent user access.
 * Each UserSession handles commands for one logged-in user.
 */
public class UserSession extends Thread {
    private final String sessionId;
    private final User user;
    private final TaskManager taskManager;
    private final UserManager userManager;
    private final BufferedReader input;
    private final PrintStream output;
    private volatile boolean running;
    private final SessionManager sessionManager;

    /**
     * Creates a new user session.
     * @param sessionId Unique session identifier
     * @param user The logged-in user
     * @param taskManager The task manager
     * @param userManager The user manager
     * @param input Input reader for commands
     * @param output Output stream for responses
     * @param sessionManager The session manager
     */
    public UserSession(String sessionId, User user, TaskManager taskManager,
                       UserManager userManager, BufferedReader input,
                       PrintStream output, SessionManager sessionManager) {
        super("UserSession-" + user.getUsername());
        this.sessionId = sessionId;
        this.user = user;
        this.taskManager = taskManager;
        this.userManager = userManager;
        this.input = input;
        this.output = output;
        this.running = true;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        output.println("\n========================================");
        output.println("Welcome, " + user.getUsername() + "!");
        output.println("Session ID: " + sessionId.substring(0, 8));
        output.println("Type 'help' for available commands.");
        output.println("========================================\n");

        while (running) {
            try {
                output.print("[" + user.getUsername() + "] > ");
                String line = input.readLine();

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line);

            } catch (IOException e) {
                output.println("Error reading input: " + e.getMessage());
                break;
            }
        }

        output.println("Session ended for " + user.getUsername());
        sessionManager.removeSession(sessionId);
    }

    /**
     * Processes a user command.
     * @param commandLine The full command line
     */
    private void processCommand(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "help":
                showHelp();
                break;
            case "add":
                addTask(args);
                break;
            case "list":
                listTasks(args);
                break;
            case "view":
                viewTask(args);
                break;
            case "complete":
                completeTask(args);
                break;
            case "progress":
                markInProgress(args);
                break;
            case "delete":
            case "remove":
                deleteTask(args);
                break;
            case "assign":
                assignTask(args);
                break;
            case "categorize":
                categorizeTask(args);
                break;
            case "categories":
                listCategories();
                break;
            case "addcategory":
                addCategory(args);
                break;
            case "users":
                listUsers();
                break;
            case "mytasks":
                listMyTasks();
                break;
            case "stats":
                showStats();
                break;
            case "whoami":
                showCurrentUser();
                break;
            case "logout":
            case "exit":
            case "quit":
                logout();
                break;
            default:
                output.println("Unknown command: " + command);
                output.println("Type 'help' for available commands.");
        }
    }

    private void showHelp() {
        output.println("\n===== Available Commands =====");
        output.println("Task Management:");
        output.println("  add <title> | <description>     - Add a new task");
        output.println("  list [all|pending|completed]    - List tasks");
        output.println("  mytasks                         - List my assigned tasks");
        output.println("  view <task-id>                  - View task details");
        output.println("  complete <task-id>              - Mark task as completed");
        output.println("  progress <task-id>              - Mark task as in progress");
        output.println("  delete <task-id>                - Delete a task");
        output.println("  assign <task-id> <username>     - Assign task to user");
        output.println("  categorize <task-id> <category> - Categorize a task");
        output.println();
        output.println("Categories:");
        output.println("  categories                      - List all categories");
        output.println("  addcategory <name>              - Add a new category");
        output.println();
        output.println("Users:");
        output.println("  users                           - List all users");
        output.println("  whoami                          - Show current user info");
        output.println();
        output.println("Other:");
        output.println("  stats                           - Show statistics");
        output.println("  logout                          - Logout and exit");
        output.println("  help                            - Show this help");
        output.println("================================\n");
    }

    private void addTask(String args) {
        if (args.isEmpty()) {
            output.println("Usage: add <title> | <description>");
            return;
        }

        String[] parts = args.split("\\|", 2);
        String title = parts[0].trim();
        String description = parts.length > 1 ? parts[1].trim() : "";

        if (title.isEmpty()) {
            output.println("Task title cannot be empty.");
            return;
        }

        Task task = taskManager.createTask(title, description, null, user.getId());
        userManager.assignTaskToUser(user.getId(), task.getId());
        output.println("Task created: [" + task.getId().substring(0, 8) + "] " + title);
    }

    private void listTasks(String filter) {
        List<Task> tasks;
        String header;

        switch (filter.toLowerCase()) {
            case "completed":
                tasks = taskManager.getTasksByStatus(TaskStatus.COMPLETED);
                header = "Completed Tasks";
                break;
            case "pending":
                tasks = taskManager.getTasksByStatus(TaskStatus.PENDING);
                header = "Pending Tasks";
                break;
            case "progress":
            case "inprogress":
                tasks = taskManager.getTasksByStatus(TaskStatus.IN_PROGRESS);
                header = "In Progress Tasks";
                break;
            case "all":
            default:
                tasks = taskManager.getAllTasks();
                header = "All Tasks";
        }

        output.println("\n===== " + header + " =====");
        if (tasks.isEmpty()) {
            output.println("No tasks found.");
        } else {
            for (Task task : tasks) {
                String assignee = "Unassigned";
                if (task.getAssignedUserId() != null) {
                    Optional<User> assignedUser = userManager.getUser(task.getAssignedUserId());
                    assignee = assignedUser.map(User::getUsername).orElse("Unknown");
                }

                String category = "None";
                if (task.getCategoryId() != null) {
                    Optional<Category> cat = taskManager.getCategory(task.getCategoryId());
                    category = cat.map(Category::getName).orElse("Unknown");
                }

                output.printf("  [%s] %-20s | Status: %-12s | Assignee: %-10s | Category: %s%n",
                        task.getId().substring(0, 8),
                        truncate(task.getTitle(), 20),
                        task.getStatus(),
                        assignee,
                        category);
            }
        }
        output.println("==========================\n");
    }

    private void listMyTasks() {
        List<Task> tasks = taskManager.getTasksByUser(user.getId());

        output.println("\n===== My Tasks =====");
        if (tasks.isEmpty()) {
            output.println("You have no assigned tasks.");
        } else {
            for (Task task : tasks) {
                output.printf("  [%s] %-25s | Status: %-12s%n",
                        task.getId().substring(0, 8),
                        truncate(task.getTitle(), 25),
                        task.getStatus());
            }
        }
        output.println("===================\n");
    }

    private void viewTask(String taskId) {
        if (taskId.isEmpty()) {
            output.println("Usage: view <task-id>");
            return;
        }

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            output.println("\n" + task.toDetailedString());

            // Add user and category names
            if (task.getAssignedUserId() != null) {
                Optional<User> assignee = userManager.getUser(task.getAssignedUserId());
                assignee.ifPresent(u -> output.println("  Assigned to: " + u.getUsername()));
            }
            if (task.getCategoryId() != null) {
                Optional<Category> cat = taskManager.getCategory(task.getCategoryId());
                cat.ifPresent(c -> output.println("  Category: " + c.getName()));
            }
        } else {
            output.println("Task not found: " + taskId);
        }
    }

    private void completeTask(String taskId) {
        if (taskId.isEmpty()) {
            output.println("Usage: complete <task-id>");
            return;
        }

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (taskManager.markTaskComplete(task.getId())) {
                output.println("Task marked as completed: " + task.getTitle());
            } else {
                output.println("Failed to complete task.");
            }
        } else {
            output.println("Task not found: " + taskId);
        }
    }

    private void markInProgress(String taskId) {
        if (taskId.isEmpty()) {
            output.println("Usage: progress <task-id>");
            return;
        }

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (taskManager.markTaskInProgress(task.getId())) {
                output.println("Task marked as in progress: " + task.getTitle());
            } else {
                output.println("Failed to update task.");
            }
        } else {
            output.println("Task not found: " + taskId);
        }
    }

    private void deleteTask(String taskId) {
        if (taskId.isEmpty()) {
            output.println("Usage: delete <task-id>");
            return;
        }

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();

            // Unassign from user if assigned
            if (task.getAssignedUserId() != null) {
                userManager.unassignTaskFromUser(task.getAssignedUserId(), task.getId());
            }

            if (taskManager.deleteTask(task.getId())) {
                output.println("Task deleted: " + task.getTitle());
            } else {
                output.println("Failed to delete task.");
            }
        } else {
            output.println("Task not found: " + taskId);
        }
    }

    private void assignTask(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            output.println("Usage: assign <task-id> <username>");
            return;
        }

        String taskId = parts[0];
        String username = parts[1];

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (!taskOpt.isPresent()) {
            output.println("Task not found: " + taskId);
            return;
        }

        Optional<User> targetUser = userManager.getUserByUsername(username);
        if (!targetUser.isPresent()) {
            output.println("User not found: " + username);
            return;
        }

        Task task = taskOpt.get();
        User target = targetUser.get();

        // Unassign from previous user
        if (task.getAssignedUserId() != null) {
            userManager.unassignTaskFromUser(task.getAssignedUserId(), task.getId());
        }

        // Assign to new user
        taskManager.assignTask(task.getId(), target.getId());
        userManager.assignTaskToUser(target.getId(), task.getId());

        output.println("Task '" + task.getTitle() + "' assigned to " + username);
    }

    private void categorizeTask(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            output.println("Usage: categorize <task-id> <category-name>");
            return;
        }

        String taskId = parts[0];
        String categoryName = parts[1];

        Optional<Task> taskOpt = findTaskByPartialId(taskId);
        if (!taskOpt.isPresent()) {
            output.println("Task not found: " + taskId);
            return;
        }

        // Find or create category
        Optional<Category> catOpt = taskManager.getCategoryByName(categoryName);
        Category category;
        if (catOpt.isPresent()) {
            category = catOpt.get();
        } else {
            category = taskManager.createCategory(categoryName);
            output.println("Created new category: " + categoryName);
        }

        Task task = taskOpt.get();
        taskManager.setTaskCategory(task.getId(), category.getId());
        output.println("Task '" + task.getTitle() + "' categorized as " + categoryName);
    }

    private void listCategories() {
        List<Category> categories = taskManager.getAllCategories();

        output.println("\n===== Categories =====");
        if (categories.isEmpty()) {
            output.println("No categories found.");
        } else {
            for (Category cat : categories) {
                int taskCount = taskManager.getTasksByCategory(cat.getId()).size();
                output.printf("  [%s] %-20s (%d tasks)%n",
                        cat.getId().substring(0, 8),
                        cat.getName(),
                        taskCount);
            }
        }
        output.println("======================\n");
    }

    private void addCategory(String name) {
        if (name.isEmpty()) {
            output.println("Usage: addcategory <name>");
            return;
        }

        if (taskManager.getCategoryByName(name).isPresent()) {
            output.println("Category already exists: " + name);
            return;
        }

        Category category = taskManager.createCategory(name);
        output.println("Category created: [" + category.getId().substring(0, 8) + "] " + name);
    }

    private void listUsers() {
        List<User> users = userManager.getAllUsers();

        output.println("\n===== Users =====");
        if (users.isEmpty()) {
            output.println("No users found.");
        } else {
            for (User u : users) {
                String active = userManager.isUserActive(u.getId()) ? " (active)" : "";
                output.printf("  %-15s | Tasks: %d%s%n",
                        u.getUsername(),
                        u.getTaskCount(),
                        active);
            }
        }
        output.println("=================\n");
    }

    private void showCurrentUser() {
        output.println("\n" + user.toDetailedString());
        output.println("  Session ID: " + sessionId.substring(0, 8));
        output.println("  Thread: " + Thread.currentThread().getName());
    }

    private void showStats() {
        output.println("\n===== Statistics =====");
        output.println("  Total Tasks: " + taskManager.getTaskCount());
        output.println("  Completed: " + taskManager.getCompletedTaskCount());
        output.println("  Pending: " + taskManager.getPendingTaskCount());
        output.println("  Categories: " + taskManager.getAllCategories().size());
        output.println("  Total Users: " + userManager.getUserCount());
        output.println("  Active Sessions: " + sessionManager.getActiveSessionCount());
        output.println("======================\n");
    }

    private void logout() {
        running = false;
        userManager.logout(user.getId());
        output.println("Goodbye, " + user.getUsername() + "!");
    }

    /**
     * Finds a task by partial ID match.
     * @param partialId The partial task ID
     * @return Optional containing the task if found
     */
    private Optional<Task> findTaskByPartialId(String partialId) {
        // First try exact match
        Optional<Task> exact = taskManager.getTask(partialId);
        if (exact.isPresent()) {
            return exact;
        }

        // Try partial match
        return taskManager.getAllTasks().stream()
                .filter(t -> t.getId().startsWith(partialId))
                .findFirst();
    }

    /**
     * Truncates a string to specified length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Stops the session.
     */
    public void stopSession() {
        running = false;
        this.interrupt();
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getUser() {
        return user;
    }

    public boolean isRunning() {
        return running;
    }
}
