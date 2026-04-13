@echo off
REM Windows Build Script for WEX Transaction Application
REM This script builds the Maven project and creates a JAR artifact

setlocal enabledelayedexpansion

echo ========================================
echo WEX Transaction Application Build Script
echo Platform: Windows
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and add it to your system PATH
    exit /b 1
)

echo Maven found at:
where mvn

REM Display Java version
echo.
echo Java version:
java -version
echo.

REM Clean previous build artifacts
echo Cleaning previous build artifacts...
call mvn clean
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven clean failed
    exit /b 1
)

echo.

REM Build the project
echo Building the project...
call mvn package
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.

REM Display build artifact location
echo Artifact location:
echo %cd%\target\wex-transaction-1.0.0.jar
echo.

REM Optional: Run the application
echo To run the application, execute:
echo java -jar target\wex-transaction-1.0.0.jar
echo.

exit /b 0
