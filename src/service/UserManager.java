package service;

import model.User;
import storage.DataStore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service class for managing users with thread-safe operations.
 * Handles user sessions and user-specific task views.
 */
public class UserManager {
    private final DataStore dataStore;
    private final ReentrantReadWriteLock lock;
    private final ConcurrentHashMap<String, String> activeSessions; // sessionId -> userId

    /**
     * Creates a UserManager with the specified data store.
     * @param dataStore The data store for persistence
     */
    public UserManager(DataStore dataStore) {
        this.dataStore = dataStore;
        this.lock = new ReentrantReadWriteLock();
        this.activeSessions = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new user.
     * @param username The username
     * @return The created user, or null if username exists
     */
    public User createUser(String username) {
        lock.writeLock().lock();
        try {
            // Check if username already exists
            if (dataStore.getUserByUsername(username).isPresent()) {
                System.out.println("[UserManager] Username already exists: " + username);
                return null;
            }

            User user = new User(username);
            dataStore.saveUser(user);
            System.out.println("[UserManager] User created: " + username);
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates a new user with email.
     * @param username The username
     * @param email The user's email
     * @return The created user, or null if username exists
     */
    public User createUser(String username, String email) {
        lock.writeLock().lock();
        try {
            // Check if username already exists
            if (dataStore.getUserByUsername(username).isPresent()) {
                System.out.println("[UserManager] Username already exists: " + username);
                return null;
            }

            User user = new User(username, email);
            dataStore.saveUser(user);
            System.out.println("[UserManager] User created: " + username);
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a user by ID.
     * @param userId The user ID
     * @return Optional containing the user if found
     */
    public Optional<User> getUser(String userId) {
        lock.readLock().lock();
        try {
            return dataStore.getUserById(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a user by username.
     * @param username The username
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByUsername(String username) {
        lock.readLock().lock();
        try {
            return dataStore.getUserByUsername(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all users.
     * @return List of all users
     */
    public List<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return dataStore.getAllUsers();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates a user's username.
     * @param userId The user ID
     * @param newUsername The new username
     * @return true if successful, false otherwise
     */
    public boolean updateUsername(String userId, String newUsername) {
        lock.writeLock().lock();
        try {
            // Check if new username already exists
            Optional<User> existingUser = dataStore.getUserByUsername(newUsername);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                System.out.println("[UserManager] Username already taken: " + newUsername);
                return false;
            }

            Optional<User> userOpt = dataStore.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setUsername(newUsername);
                dataStore.updateUser(user);
                System.out.println("[UserManager] Username updated: " + newUsername);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates a user's email.
     * @param userId The user ID
     * @param newEmail The new email
     * @return true if successful, false otherwise
     */
    public boolean updateEmail(String userId, String newEmail) {
        lock.writeLock().lock();
        try {
            Optional<User> userOpt = dataStore.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setEmail(newEmail);
                dataStore.updateUser(user);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a user.
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    public boolean deleteUser(String userId) {
        lock.writeLock().lock();
        try {
            Optional<User> userOpt = dataStore.getUserById(userId);
            if (userOpt.isPresent()) {
                dataStore.deleteUser(userId);
                System.out.println("[UserManager] User deleted: " + userId.substring(0, 8));
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Logs in a user and creates a session.
     * @param username The username
     * @return The user if found, null otherwise
     */
    public User login(String username) {
        lock.writeLock().lock();
        try {
            Optional<User> userOpt = dataStore.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.updateLastLogin();
                dataStore.updateUser(user);

                // Create session
                String sessionId = java.util.UUID.randomUUID().toString();
                activeSessions.put(sessionId, user.getId());

                System.out.println("[UserManager] User logged in: " + username);
                return user;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Logs out a user and removes their session.
     * @param userId The user ID
     */
    public void logout(String userId) {
        // Remove all sessions for this user
        activeSessions.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        System.out.println("[UserManager] User logged out: " + userId.substring(0, 8));
    }

    /**
     * Assigns a task to a user.
     * @param userId The user ID
     * @param taskId The task ID
     * @return true if successful, false otherwise
     */
    public boolean assignTaskToUser(String userId, String taskId) {
        lock.writeLock().lock();
        try {
            Optional<User> userOpt = dataStore.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.assignTask(taskId);
                dataStore.updateUser(user);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a task assignment from a user.
     * @param userId The user ID
     * @param taskId The task ID
     * @return true if successful, false otherwise
     */
    public boolean unassignTaskFromUser(String userId, String taskId) {
        lock.writeLock().lock();
        try {
            Optional<User> userOpt = dataStore.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.unassignTask(taskId);
                dataStore.updateUser(user);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the number of active users.
     * @return Number of users with active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Checks if a user has an active session.
     * @param userId The user ID
     * @return true if user has active session
     */
    public boolean isUserActive(String userId) {
        return activeSessions.containsValue(userId);
    }

    /**
     * Gets total user count.
     * @return Total number of users
     */
    public int getUserCount() {
        lock.readLock().lock();
        try {
            return dataStore.getAllUsers().size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
