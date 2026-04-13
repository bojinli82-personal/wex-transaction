#!/bin/bash

# Build Script for WEX Transaction Application (Linux/macOS)
# This script builds the Maven project and creates a JAR artifact

set -e  # Exit on error

echo "========================================"
echo "WEX Transaction Application Build Script"
echo "Platform: Linux/macOS"
echo "========================================"
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven:"
    echo "  - macOS: brew install maven"
    echo "  - Ubuntu/Debian: sudo apt-get install maven"
    echo "  - CentOS/RHEL: sudo yum install maven"
    exit 1
fi

echo "Maven found at:"
which mvn
echo

# Display Java version
echo "Java version:"
java -version
echo

# Clean previous build artifacts
echo "Cleaning previous build artifacts..."
mvn clean

echo

# Build the project
echo "Building the project..."
mvn package

echo
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo

# Display build artifact location
echo "Artifact location:"
echo "$(pwd)/target/wex-transaction-1.0.0.jar"
echo

# Optional: Run the application
echo "To run the application, execute:"
echo "java -jar target/wex-transaction-1.0.0.jar"
echo

exit 0
