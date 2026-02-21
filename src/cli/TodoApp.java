package cli;

import concurrency.SessionManager;
import model.Category;
import model.User;
import service.TaskManager;
import service.UserManager;
import storage.DataStore;
import storage.FileDataStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Optional;

/**
 * Main entry point for the Collaborative To-Do List Application.
 * This CLI application supports multiple concurrent users with thread-safe operations.
 *
 * Features:
 * - Multi-user support with concurrent sessions
 * - Task creation, modification, and deletion
 * - Task categorization and assignment
 * - Status tracking (Pending, In Progress, Completed)
 * - Persistent data storage
 *
 * Demonstrates:
 * - Object-Oriented Programming principles (classes, encapsulation, inheritance)
 * - Java threading for concurrent user sessions
 * - Thread-safe data structures (ConcurrentHashMap, ReentrantReadWriteLock)
 * - File-based persistence
 */
public class TodoApp {
    private static final String DATA_DIR = "data";

    private final DataStore dataStore;
    private final TaskManager taskManager;
    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final BufferedReader reader;
    private final PrintStream output;
    private volatile boolean running;

    /**
     * Creates a new TodoApp instance.
     */
    public TodoApp() {
        this.dataStore = new FileDataStore(DATA_DIR);
        this.taskManager = new TaskManager(dataStore);
        this.userManager = new UserManager(dataStore);
        this.sessionManager = new SessionManager(taskManager, userManager);
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.output = System.out;
        this.running = true;
    }

    /**
     * Starts the application.
     */
    public void start() {
        printBanner();

        // Load existing data
        dataStore.loadAll();

        // Create default categories if none exist
        createDefaultCategories();

        output.println("Data loaded. " + userManager.getUserCount() + " users, " +
                taskManager.getTaskCount() + " tasks found.\n");

        // Main loop for user login/registration
        while (running) {
            try {
                showMainMenu();
                String input = reader.readLine();

                if (input == null) {
                    break;
                }

                handleMainMenuChoice(input.trim());

            } catch (Exception e) {
                output.println("Error: " + e.getMessage());
            }
        }

        shutdown();
    }

    private void printBanner() {
        output.println("╔══════════════════════════════════════════════════════════════╗");
        output.println("║        COLLABORATIVE TO-DO LIST APPLICATION                  ║");
        output.println("║              Multi-User Task Management System               ║");
        output.println("╚══════════════════════════════════════════════════════════════╝");
        output.println();
    }

    private void showMainMenu() {
        output.println("===== Main Menu =====");
        output.println("1. Login");
        output.println("2. Register new user");
        output.println("3. List users");
        output.println("4. Demo: Run concurrent users simulation");
        output.println("5. Exit");
        output.println("=====================");
        output.print("Choose an option: ");
    }

    private void handleMainMenuChoice(String choice) throws Exception {
        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                handleRegister();
                break;
            case "3":
                listAllUsers();
                break;
            case "4":
                runConcurrencyDemo();
                break;
            case "5":
            case "exit":
            case "quit":
                running = false;
                break;
            default:
                output.println("Invalid option. Please try again.");
        }
    }

    private void handleLogin() throws Exception {
        output.print("Enter username: ");
        String username = reader.readLine();

        if (username == null || username.trim().isEmpty()) {
            output.println("Username cannot be empty.");
            return;
        }

        User user = userManager.login(username.trim());
        if (user == null) {
            output.println("User not found. Would you like to register? (y/n): ");
            String response = reader.readLine();
            if (response != null && response.toLowerCase().startsWith("y")) {
                user = userManager.createUser(username.trim());
                if (user != null) {
                    startUserSession(user);
                }
            }
        } else {
            startUserSession(user);
        }
    }

    private void handleRegister() throws Exception {
        output.print("Enter new username: ");
        String username = reader.readLine();

        if (username == null || username.trim().isEmpty()) {
            output.println("Username cannot be empty.");
            return;
        }

        output.print("Enter email (optional, press Enter to skip): ");
        String email = reader.readLine();

        User user;
        if (email != null && !email.trim().isEmpty()) {
            user = userManager.createUser(username.trim(), email.trim());
        } else {
            user = userManager.createUser(username.trim());
        }

        if (user != null) {
            output.println("User registered successfully!");
            output.print("Would you like to login now? (y/n): ");
            String response = reader.readLine();
            if (response != null && response.toLowerCase().startsWith("y")) {
                startUserSession(user);
            }
        } else {
            output.println("Failed to register user. Username may already exist.");
        }
    }

    private void startUserSession(User user) {
        // Create session and wait for it to complete
        String sessionId = sessionManager.createSession(user, reader, output);

        // Wait for the session to complete
        try {
            concurrency.UserSession session = sessionManager.getSession(sessionId);
            if (session != null) {
                session.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void listAllUsers() {
        output.println("\n===== Registered Users =====");
        var users = userManager.getAllUsers();
        if (users.isEmpty()) {
            output.println("No users registered.");
        } else {
            for (User user : users) {
                output.printf("  %-15s | Tasks: %d%n", user.getUsername(), user.getTaskCount());
            }
        }
        output.println("=============================\n");
    }

    /**
     * Demonstrates concurrent user access by simulating multiple users
     * performing operations simultaneously.
     */
    private void runConcurrencyDemo() {
        output.println("\n===== Concurrency Demonstration =====");
        output.println("This demo creates multiple threads that simultaneously");
        output.println("create and modify tasks, demonstrating thread-safe operations.\n");

        // Ensure we have demo users
        User user1 = getOrCreateUser("DemoUser1");
        User user2 = getOrCreateUser("DemoUser2");
        User user3 = getOrCreateUser("DemoUser3");

        // Create demo category
        Category workCategory = taskManager.getCategoryByName("Work")
                .orElseGet(() -> taskManager.createCategory("Work", "Work-related tasks"));

        // Create worker threads
        Thread worker1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    output.println("[Thread-1] Creating task " + i);
                    var task = taskManager.createTask("Demo Task 1-" + i,
                            "Created by thread 1", workCategory.getId(), user1.getId());
                    Thread.sleep(100);
                    output.println("[Thread-1] Marking task " + i + " in progress");
                    taskManager.markTaskInProgress(task.getId());
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "DemoWorker-1");

        Thread worker2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    output.println("[Thread-2] Creating task " + i);
                    var task = taskManager.createTask("Demo Task 2-" + i,
                            "Created by thread 2", workCategory.getId(), user2.getId());
                    Thread.sleep(80);
                    output.println("[Thread-2] Completing task " + i);
                    taskManager.markTaskComplete(task.getId());
                    Thread.sleep(70);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "DemoWorker-2");

        Thread worker3 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    output.println("[Thread-3] Creating task " + i);
                    var task = taskManager.createTask("Demo Task 3-" + i,
                            "Created by thread 3", null, user3.getId());
                    Thread.sleep(60);
                    output.println("[Thread-3] Reading all tasks");
                    int count = taskManager.getTaskCount();
                    output.println("[Thread-3] Current task count: " + count);
                    Thread.sleep(90);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "DemoWorker-3");

        // Start all threads
        output.println("Starting 3 concurrent worker threads...\n");
        worker1.start();
        worker2.start();
        worker3.start();

        // Wait for all threads to complete
        try {
            worker1.join();
            worker2.join();
            worker3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        output.println("\n===== Demo Complete =====");
        output.println("Total tasks after demo: " + taskManager.getTaskCount());
        output.println("Completed tasks: " + taskManager.getCompletedTaskCount());
        output.println("Pending tasks: " + taskManager.getPendingTaskCount());
        output.println("========================\n");
    }

    private User getOrCreateUser(String username) {
        return userManager.getUserByUsername(username)
                .orElseGet(() -> userManager.createUser(username));
    }

    private void createDefaultCategories() {
        if (taskManager.getAllCategories().isEmpty()) {
            taskManager.createCategory("Work", "Work-related tasks");
            taskManager.createCategory("Personal", "Personal tasks");
            taskManager.createCategory("Shopping", "Shopping lists");
            taskManager.createCategory("Urgent", "High-priority urgent tasks");
            output.println("Default categories created.");
        }
    }

    private void shutdown() {
        output.println("\nShutting down...");
        sessionManager.stopAllSessions();
        dataStore.persistAll();
        output.println("Data saved. Goodbye!");
    }

    /**
     * Main entry point.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        TodoApp app = new TodoApp();

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nReceived shutdown signal...");
        }));

        app.start();
    }
}
