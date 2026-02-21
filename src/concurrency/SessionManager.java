package concurrency;

import model.User;
import service.TaskManager;
import service.UserManager;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple concurrent user sessions.
 * Demonstrates thread management and concurrent collection usage.
 */
public class SessionManager {
    private final Map<String, UserSession> activeSessions;
    private final TaskManager taskManager;
    private final UserManager userManager;

    public SessionManager(TaskManager taskManager, UserManager userManager) {
        this.activeSessions = new ConcurrentHashMap<>();
        this.taskManager = taskManager;
        this.userManager = userManager;
    }

    public String createSession(User user, BufferedReader input, PrintStream output) {
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(
                sessionId, user, taskManager, userManager, input, output, this);
        activeSessions.put(sessionId, session);
        session.start();
        System.out.println("[SessionManager] Session created: " + sessionId.substring(0, 8) +
                " for user: " + user.getUsername());
        return sessionId;
    }

    public void removeSession(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            System.out.println("[SessionManager] Session removed: " + sessionId.substring(0, 8));
        }
    }

    public UserSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public void stopSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.stopSession();
            activeSessions.remove(sessionId);
        }
    }

    public void stopAllSessions() {
        System.out.println("[SessionManager] Stopping all sessions...");
        for (UserSession session : activeSessions.values()) {
            session.stopSession();
        }
        activeSessions.clear();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public boolean hasActiveSession(String userId) {
        return activeSessions.values().stream()
                .anyMatch(session -> session.getUser().getId().equals(userId));
    }

    public UserSession getSessionByUserId(String userId) {
        return activeSessions.values().stream()
                .filter(session -> session.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    public void waitForAllSessions() {
        for (UserSession session : activeSessions.values()) {
            try {
                session.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void printSessionStatus() {
        System.out.println("\n===== Active Sessions =====");
        if (activeSessions.isEmpty()) {
            System.out.println("No active sessions.");
        } else {
            for (UserSession session : activeSessions.values()) {
                System.out.printf("  Session: %s | User: %s | Thread: %s | Running: %s%n",
                        session.getSessionId().substring(0, 8),
                        session.getUser().getUsername(),
                        session.getName(),
                        session.isRunning());
            }
        }
        System.out.println("===========================\n");
    }
}
