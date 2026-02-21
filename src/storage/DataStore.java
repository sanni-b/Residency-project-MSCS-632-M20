package storage;

import model.Category;
import model.Task;
import model.User;

import java.util.List;
import java.util.Optional;

/**
 * Interface defining data storage operations for the to-do list application.
 * Follows the Repository pattern for data access abstraction.
 */
public interface DataStore {

    // Task operations
    void saveTask(Task task);
    Optional<Task> getTaskById(String id);
    List<Task> getAllTasks();
    List<Task> getTasksByUserId(String userId);
    List<Task> getTasksByCategoryId(String categoryId);
    void deleteTask(String id);
    void updateTask(Task task);

    // User operations
    void saveUser(User user);
    Optional<User> getUserById(String id);
    Optional<User> getUserByUsername(String username);
    List<User> getAllUsers();
    void deleteUser(String id);
    void updateUser(User user);

    // Category operations
    void saveCategory(Category category);
    Optional<Category> getCategoryById(String id);
    Optional<Category> getCategoryByName(String name);
    List<Category> getAllCategories();
    void deleteCategory(String id);
    void updateCategory(Category category);

    // Persistence operations
    void persistAll();
    void loadAll();
    void clear();
}
