



#!/bin/bash

# Collaborative To-Do List Application
# React Frontend Start Script

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$PROJECT_DIR/frontend"

echo "======================================"
echo "Collaborative To-Do List Frontend"
echo "======================================"
echo ""

cd "$FRONTEND_DIR"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo "Starting React development server..."
npm start
