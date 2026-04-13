
@echo off
REM Windows Start Script for WEX Transaction Application
REM This script starts the WEX Transaction Spring Boot application

setlocal enabledelayedexpansion

echo ========================================
echo WEX Transaction Application - Start
echo Platform: Windows
echo ========================================
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher
    exit /b 1
)

echo Java version:
java -version
echo.

REM Define the JAR file path
set JAR_FILE=target\wex-transaction-1.0.0.jar

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found: %JAR_FILE%
    echo Please run build.bat first to create the JAR artifact
    exit /b 1
)

echo Starting WEX Transaction Application...
echo JAR: %JAR_FILE%
echo.

REM Start the application with optional parameters
REM You can pass JVM arguments and application arguments
REM Examples:
REM   start.bat                          - Start with defaults
REM   start.bat --server.port=8090       - Start on custom port
REM   start.bat --spring.profiles.active=prod - Use production profile

java -jar "%JAR_FILE%" %*

echo.
echo Application stopped.
exit /b 0
