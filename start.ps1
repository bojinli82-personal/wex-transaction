# Windows PowerShell Start Script for WEX Transaction Application
# This script starts the WEX Transaction Spring Boot application
# Usage: .\start.ps1 or .\start.ps1 --server.port=8090

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$Arguments
)

Write-Host "========================================"
Write-Host "WEX Transaction Application - Start"
Write-Host "Platform: Windows (PowerShell)"
Write-Host "========================================"
Write-Host ""

# Check if Java is installed
$javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaPath) {
    Write-Host "ERROR: Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 21 or higher"
    exit 1
}

Write-Host "Java version:"
java -version
Write-Host ""

# Define the JAR file path
$jarFile = "target\wex-transaction-1.0.0.jar"

# Check if JAR file exists
if (-not (Test-Path $jarFile)) {
    Write-Host "ERROR: JAR file not found: $jarFile" -ForegroundColor Red
    Write-Host "Please run .\build.bat or .\build.ps1 first to create the JAR artifact"
    exit 1
}

Write-Host "Starting WEX Transaction Application..."
Write-Host "JAR: $jarFile"
Write-Host ""

# Start the application with optional parameters
# You can pass JVM arguments and application arguments
# Examples:
#   .\start.ps1                          - Start with defaults
#   .\start.ps1 --server.port=8090       - Start on custom port
#   .\start.ps1 --spring.profiles.active=prod - Use production profile

& java -jar $jarFile @Arguments

Write-Host ""
Write-Host "Application stopped."
exit 0
