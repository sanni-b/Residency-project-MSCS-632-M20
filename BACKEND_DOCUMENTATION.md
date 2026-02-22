# Collaborative To-Do List Application - Backend Documentation

## 📋 Overview

A **multi-user collaborative to-do list application** built with Java demonstrating:
- Object-Oriented Programming (OOP) principles
- Thread-safe concurrent operations
- RESTful API design
- File-based persistence
- Clean architecture patterns

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│                    http://localhost:3000                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ HTTP REST API
┌─────────────────────────────────────────────────────────────────┐
│                      API Layer (ApiServer)                      │
│                    http://localhost:8080                        │
├─────────────────────────────────────────────────────────────────┤
│                     Service Layer                               │
│              ┌──────────────┬──────────────┐                    │
│              │ TaskManager  │ UserManager  │                    │
│              └──────────────┴──────────────┘                    │
├─────────────────────────────────────────────────────────────────┤
│                     Storage Layer                               │
│              ┌──────────────────────────┐                       │
│              │  DataStore (Interface)   │                       │
│              │    FileDataStore (Impl)  │                       │
│              └──────────────────────────┘                       │
├─────────────────────────────────────────────────────────────────┤
│                      Model Layer                                │
│     ┌──────┬──────┬──────────┬────────────┬──────────┐         │
│     │ Task │ User │ Category │ TaskStatus │ Priority │         │
│     └──────┴──────┴──────────┴────────────┴──────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📁 Package Structure

```
src/
├── api/                    # REST API Layer
│   └── ApiServer.java      # HTTP server with endpoints
├── cli/                    # Command Line Interface
│   └── TodoApp.java        # CLI application entry point
├── concurrency/            # Threading & Session Management
│   ├── SessionManager.java # Manages concurrent user sessions
│   └── UserSession.java    # Individual user thread session
├── model/                  # Domain Models (POJOs)
│   ├── Task.java           # Task entity
│   ├── TaskStatus.java     # Task status enum
│   ├── Priority.java       # Task priority enum
│   ├── User.java           # User entity
│   └── Category.java       # Category entity
├── service/                # Business Logic Layer
│   ├── TaskManager.java    # Task operations service
│   └── UserManager.java    # User operations service
└── storage/                # Data Persistence Layer
    ├── DataStore.java      # Storage interface
    └── FileDataStore.java  # File-based implementation
```

---

## 🔷 Model Layer (`src/model/`)

### Task.java
**Purpose:** Core entity representing a to-do item.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String (UUID) | Unique identifier |
| `title` | String | Task title |
| `description` | String | Detailed description |
| `categoryId` | String | Foreign key to Category |
| `assignedUserId` | String | Foreign key to User |
| `status` | TaskStatus | Current status (PENDING/IN_PROGRESS/COMPLETED) |
| `priority` | Priority | Priority level (LOW/MEDIUM/HIGH/URGENT) |
| `dueDate` | LocalDateTime | Optional due date |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |
| `completedAt` | LocalDateTime | Completion timestamp |

**Key Methods:**
- `markAsCompleted()` - Marks task as done
- `markAsInProgress()` - Sets task to in-progress
- `isOverdue()` - Checks if task is past due date
- `isCompleted()` - Status check helper

---

### TaskStatus.java (Enum)
**Purpose:** Type-safe status tracking.

| Value | Display Name | Description |
|-------|--------------|-------------|
| `PENDING` | "Pending" | Not started |
| `IN_PROGRESS` | "In Progress" | Currently working |
| `COMPLETED` | "Completed" | Done |

---

### Priority.java (Enum)
**Purpose:** Task priority levels with UI color codes.

| Value | Level | Color | Use Case |
|-------|-------|-------|----------|
| `LOW` | 1 | 🟢 #10b981 | Non-urgent tasks |
| `MEDIUM` | 2 | 🟡 #f59e0b | Normal tasks |
| `HIGH` | 3 | 🟠 #f97316 | Important tasks |
| `URGENT` | 4 | 🔴 #ef4444 | Critical/deadline tasks |

---

### User.java
**Purpose:** Represents application users.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String (UUID) | Unique identifier |
| `username` | String | Unique username |
| `email` | String | Email address |
| `createdAt` | LocalDateTime | Registration date |
| `lastLoginAt` | LocalDateTime | Last activity |
| `assignedTaskIds` | List<String> | Tasks assigned to user |

**Key Methods:**
- `assignTask(taskId)` - Assign task to user
- `unassignTask(taskId)` - Remove task assignment
- `updateLastLogin()` - Update activity timestamp

---

### Category.java
**Purpose:** Organize tasks into groups.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String (UUID) | Unique identifier |
| `name` | String | Category name (Work, Personal, etc.) |
| `description` | String | Category description |

**Default Categories:** Work, Personal, Shopping, Health, Learning

---

## ⚙️ Service Layer (`src/service/`)

### TaskManager.java
**Purpose:** Business logic for task operations with **thread-safe** access.

**Concurrency:** Uses `ReentrantReadWriteLock` for safe concurrent access.

| Method | Description | Lock Type |
|--------|-------------|-----------|
| `createTask(title, description)` | Create new task | Write |
| `createTask(title, desc, categoryId, userId)` | Create with assignments | Write |
| `getTask(taskId)` | Get task by ID | Read |
| `getAllTasks()` | Get all tasks | Read |
| `getTasksByUser(userId)` | Filter by user | Read |
| `getTasksByCategory(categoryId)` | Filter by category | Read |
| `updateTaskTitle(taskId, title)` | Update title | Write |
| `setTaskStatus(taskId, status)` | Change status | Write |
| `assignTask(taskId, userId)` | Assign to user | Write |
| `deleteTask(taskId)` | Remove task | Write |

---

### UserManager.java
**Purpose:** User management with session handling.

**Concurrency:** Uses `ReentrantReadWriteLock` + `ConcurrentHashMap` for sessions.

| Method | Description | Lock Type |
|--------|-------------|-----------|
| `createUser(username)` | Create user | Write |
| `createUser(username, email)` | Create with email | Write |
| `getUser(userId)` | Get by ID | Read |
| `getUserByUsername(username)` | Get by username | Read |
| `getAllUsers()` | List all users | Read |
| `deleteUser(userId)` | Remove user | Write |
| `authenticateUser(username)` | Login user | Write |

---

## 💾 Storage Layer (`src/storage/`)

### DataStore.java (Interface)
**Purpose:** Abstraction for data persistence (Repository Pattern).

```java
// Task Operations
void saveTask(Task task);
Optional<Task> getTaskById(String id);
List<Task> getAllTasks();
void deleteTask(String id);

// User Operations
void saveUser(User user);
Optional<User> getUserById(String id);
void deleteUser(String id);

// Category Operations
void saveCategory(Category category);
List<Category> getAllCategories();

// Persistence
void persistAll();  // Save to disk
void loadAll();     // Load from disk
```

---

### FileDataStore.java
**Purpose:** File-based implementation using custom text format.

**Data Files:**
| File | Content |
|------|---------|
| `data/tasks.dat` | Serialized tasks |
| `data/users.dat` | Serialized users |
| `data/categories.dat` | Serialized categories |

**Format:** Pipe-delimited (`|`) with escaped special characters.

**Thread Safety:** Uses `ReentrantReadWriteLock` for file operations.

**Features:**
- Auto-creates data directory
- Backwards-compatible deserialization
- Automatic persistence on changes

---

## 🌐 API Layer (`src/api/`)

### ApiServer.java
**Purpose:** REST API server for React frontend.

**Port:** `8080`  
**Base URL:** `http://localhost:8080/api`

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/tasks` | Get all tasks |
| `GET` | `/api/tasks/:id` | Get task by ID |
| `POST` | `/api/tasks` | Create task |
| `PUT` | `/api/tasks/:id` | Update task |
| `DELETE` | `/api/tasks/:id` | Delete task |
| `GET` | `/api/users` | Get all users |
| `GET` | `/api/users/:id` | Get user by ID |
| `POST` | `/api/users` | Create user |
| `DELETE` | `/api/users/:id` | Delete user |
| `GET` | `/api/categories` | Get all categories |

#### Request/Response Examples

**Create Task:**
```json
POST /api/tasks
{
  "title": "Complete project",
  "description": "Finish the documentation",
  "categoryId": "uuid-here",
  "assignedUserId": "uuid-here",
  "priority": "HIGH"
}
```

**Task Response:**
```json
{
  "id": "uuid",
  "title": "Complete project",
  "description": "Finish the documentation",
  "categoryId": "uuid",
  "assignedUserId": "uuid",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": null,
  "isOverdue": false,
  "createdAt": "2026-02-22T10:30:00",
  "updatedAt": "2026-02-22T10:30:00",
  "completedAt": null
}
```

**CORS:** Enabled for all origins (`*`)

---

## 🔄 Concurrency Layer (`src/concurrency/`)

### SessionManager.java
**Purpose:** Manages multiple concurrent CLI user sessions.

| Method | Description |
|--------|-------------|
| `createSession(user, input, output)` | Start new session thread |
| `removeSession(sessionId)` | End session |
| `stopAllSessions()` | Shutdown all |
| `getActiveSessionCount()` | Count active sessions |
| `hasActiveSession(userId)` | Check if user is online |

---

### UserSession.java
**Purpose:** Individual user thread for CLI interface.

**Extends:** `Thread`

**Features:**
- Isolated command processing per user
- Interactive CLI with prompts
- Thread-safe task operations
- Auto-cleanup on exit

**Commands Available:**
| Command | Description |
|---------|-------------|
| `add <title>` | Add new task |
| `list` | Show all tasks |
| `my` | Show my tasks |
| `done <id>` | Mark task complete |
| `delete <id>` | Remove task |
| `status` | Session info |
| `help` | Show commands |
| `exit` | Logout |

---

## 🔒 Thread Safety Summary

| Component | Mechanism | Purpose |
|-----------|-----------|---------|
| TaskManager | `ReentrantReadWriteLock` | Safe concurrent task access |
| UserManager | `ReentrantReadWriteLock` + `ConcurrentHashMap` | Safe user + session access |
| FileDataStore | `ReentrantReadWriteLock` | Safe file I/O |
| SessionManager | `ConcurrentHashMap` | Safe session registry |
| UserSession | `volatile boolean` | Safe thread stopping |

---

## 🚀 Running the Application

### Start API Server (for React frontend):
```bash
./run-api.sh
# or
java -cp out api.ApiServer
```

### Start CLI Application:
```bash
./run.sh
# or
java -cp out cli.TodoApp
```

### Start Frontend:
```bash
./run-frontend.sh
# or
cd frontend && npm start
```

---

## 📊 Design Patterns Used

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Repository** | DataStore interface | Abstract data access |
| **Service Layer** | TaskManager, UserManager | Business logic separation |
| **Singleton-like** | ApiServer's managers | Single instance per server |
| **Factory** | UUID generation in models | Object creation |
| **Observer-like** | Timestamp updates | Auto-update on changes |

---

## 🔧 OOP Principles Demonstrated

1. **Encapsulation:** Private fields with public getters/setters
2. **Abstraction:** DataStore interface hides implementation
3. **Inheritance:** UserSession extends Thread
4. **Polymorphism:** DataStore implementations are swappable
5. **Composition:** Managers contain DataStore reference

---

*Generated: February 22, 2026*
