# Collaborative To-Do List Application

A command-line multi-user to-do list application built in Java, demonstrating object-oriented programming principles and thread-based concurrency.

## Features

- **Multi-User Support**: Multiple users can register and manage their tasks
- **Concurrent Access**: Thread-safe operations allow multiple users to access and edit tasks simultaneously
- **Task Management**: Create, view, update, delete, and complete tasks
- **Task Categorization**: Organize tasks into categories (Work, Personal, Shopping, etc.)
- **Task Assignment**: Assign tasks to specific users
- **Status Tracking**: Track task status (Pending, In Progress, Completed)
- **Persistent Storage**: Data is saved to files and persists between sessions

## Project Structure

```
src/
├── model/           # Data model classes
│   ├── Task.java         - Task entity with status, category, and assignment
│   ├── User.java         - User entity with assigned tasks
│   ├── Category.java     - Category for task organization
│   └── TaskStatus.java   - Enum for task states (PENDING, IN_PROGRESS, COMPLETED)
│
├── service/         # Business logic layer (thread-safe)
│   ├── TaskManager.java  - Thread-safe task operations with ReentrantReadWriteLock
│   └── UserManager.java  - Thread-safe user operations and session management
│
├── storage/         # Data persistence layer
│   ├── DataStore.java    - Storage interface (Repository pattern)
│   └── FileDataStore.java - File-based implementation with ConcurrentHashMap
│
├── concurrency/     # Threading and session management
│   ├── UserSession.java  - Individual user session thread (extends Thread)
│   └── SessionManager.java - Manages multiple concurrent user sessions
│
└── cli/             # Command-line interface
    └── TodoApp.java      - Main application entry point

data/                # Data storage directory (auto-created)
├── tasks.dat        - Persisted tasks
├── users.dat        - Persisted users
└── categories.dat   - Persisted categories
```

## OOP Principles Demonstrated

1. **Encapsulation**: All model classes use private fields with getters/setters
2. **Abstraction**: DataStore interface abstracts storage implementation
3. **Inheritance**: UserSession extends Thread for concurrent execution
4. **Polymorphism**: DataStore interface allows different storage implementations

## Concurrency Features

- **ReentrantReadWriteLock**: Used in TaskManager and UserManager for thread-safe read/write operations
- **ConcurrentHashMap**: Thread-safe collections for in-memory data storage
- **Thread-per-Session**: Each user session runs in its own thread
- **Volatile Variables**: Used for thread-safe flag checking

## Building and Running

### Prerequisites
- Java JDK 11 or higher

### Compile
```bash
cd /Users/sunny/Residency-project-MSCS-632-M20
javac -d out src/model/*.java src/storage/*.java src/service/*.java src/concurrency/*.java src/cli/*.java
```

### Run
```bash
java -cp out cli.TodoApp
```

### Or use the provided script
```bash
./run.sh
```

## User Session Commands

### Task Management
- `add <title> | <description>` - Add a new task
- `list [all|pending|completed]` - List tasks
- `mytasks` - List your assigned tasks
- `view <task-id>` - View task details
- `complete <task-id>` - Mark task as completed
- `progress <task-id>` - Mark task as in progress
- `delete <task-id>` - Delete a task

### Task Organization
- `assign <task-id> <username>` - Assign task to user
- `categorize <task-id> <category>` - Set task category
- `categories` - List all categories
- `addcategory <name>` - Create new category

### Other Commands
- `users` - List all users
- `whoami` - Show current user info
- `stats` - Show statistics
- `logout` - End session and logout
- `help` - Show available commands

## Example Session

```
Choose an option: 1
Enter username: john

========================================
Welcome, john!
Type 'help' for available commands.
========================================

[john] > add Buy groceries | Milk, eggs, bread
Task created: [a1b2c3d4] Buy groceries

[john] > categorize a1b2 Shopping
Task 'Buy groceries' categorized as Shopping

[john] > complete a1b2
Task marked as completed: Buy groceries

[john] > stats
===== Statistics =====
  Total Tasks: 1
  Completed: 1
  Pending: 0
======================

[john] > logout
Goodbye, john!
```

## Author

MSCS-632-M20 Residency Project
