#!/bin/bash

# Start Script for WEX Transaction Application (Linux/macOS)
# This script starts the WEX Transaction Spring Boot application

set -e  # Exit on error

echo "========================================"
echo "WEX Transaction Application - Start"
echo "Platform: Linux/macOS"
echo "========================================"
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 21 or higher:"
    echo "  - macOS: brew install openjdk@21"
    echo "  - Ubuntu/Debian: sudo apt-get install openjdk-21-jre-headless"
    echo "  - CentOS/RHEL: sudo yum install java-21-openjdk"
    exit 1
fi

echo "Java version:"
java -version
echo

# Define the JAR file path
JAR_FILE="target/wex-transaction-1.0.0.jar"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    echo "Please run ./build.sh first to create the JAR artifact"
    exit 1
fi

echo "Starting WEX Transaction Application..."
echo "JAR: $JAR_FILE"
echo

# Start the application with optional parameters
# You can pass JVM arguments and application arguments
# Examples:
#   ./start.sh                          - Start with defaults
#   ./start.sh --server.port=8090       - Start on custom port
#   ./start.sh --spring.profiles.active=prod - Use production profile

java -jar "$JAR_FILE" "$@"

echo
echo "Application stopped."
exit 0
