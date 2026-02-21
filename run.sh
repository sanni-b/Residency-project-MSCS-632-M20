#!/bin/bash

# Collaborative To-Do List Application
# Build and Run Script

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"

echo "======================================"
echo "Collaborative To-Do List Application"
echo "======================================"
echo ""

# Create output directory if it doesn't exist
mkdir -p "$OUT_DIR"

# Compile all Java files
echo "Compiling Java files..."
javac -d "$OUT_DIR" \
    "$SRC_DIR/model/"*.java \
    "$SRC_DIR/storage/"*.java \
    "$SRC_DIR/service/"*.java \
    "$SRC_DIR/concurrency/"*.java \
    "$SRC_DIR/cli/"*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "Starting application..."
    echo ""
    # Run the application
    java -cp "$OUT_DIR" cli.TodoApp
else
    echo "Compilation failed!"
    exit 1
fi
