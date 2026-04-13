@echo off
REM ============================================
REM WEX Transaction - Run Script (Windows)
REM ============================================
REM Usage: run.bat [options]
REM
REM Options:
REM   start              Start application (default)
REM   stop               Stop application
REM   restart            Restart application
REM   status             Check status
REM   logs               View logs
REM   debug              Start in debug mode (port 5005)
REM   help               Show this help message
REM
REM Environment Variables:
REM   PORT               HTTP port (default: 8080)
REM   DEBUG_PORT         Debug port (default: 5005)
REM ============================================

setlocal enabledelayedexpansion

REM Configuration
set "APP_NAME=wex-transaction"
set "JAR_FILE=target\wex-transaction-1.0.0.jar"
set "LOG_DIR=logs"
set "LOG_FILE=%LOG_DIR%\%APP_NAME%.log"
set "PID_FILE=%LOG_DIR%\%APP_NAME%.pid"
set "PORT=%PORT:=8080%"
set "DEBUG_PORT=%DEBUG_PORT:=5005%"

REM Colors using ANSI escape codes (Windows 10+)
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM ============================================
REM Functions
REM ============================================

:print_header
    echo.
    echo %GREEN%════════════════════════════════════════%NC%
    echo %GREEN%%~1%NC%
    echo %GREEN%════════════════════════════════════════%NC%
    echo.
    goto :eof

:print_error
    echo %RED%X ERROR: %~1%NC%
    goto :eof

:print_success
    echo %GREEN%! SUCCESS: %~1%NC%
    goto :eof

:print_info
    echo %YELLOW%i INFO: %~1%NC%
    goto :eof

:print_url
    echo %BLUE%-^> %~1%NC%
    goto :eof

:show_help
    cls
    echo WEX Transaction Application Launcher
    echo.
    echo Usage: run.bat [options]
    echo.
    echo Options:
    echo   start (default)    Start application
    echo   stop               Stop application
    echo   restart            Restart application
    echo   status             Check application status
    echo   logs               View application logs
    echo   debug              Start in debug mode (port 5005)
    echo   help               Show this help message
    echo.
    echo Examples:
    echo   run.bat                    # Start application
    echo   run.bat stop               # Stop application
    echo   run.bat restart            # Restart application
    echo   run.bat logs               # View logs
    echo   run.bat debug              # Debug mode
    echo.
    echo Environment Variables:
    echo   PORT               HTTP port (default: 8080)
    echo   DEBUG_PORT         Debug port (default: 5005)
    echo.
    echo URLs:
    echo   Health Check       http://localhost:8080/api/actuator/health
    echo   Metrics            http://localhost:8080/api/actuator/metrics
    echo   Transactions       http://localhost:8080/api/transactions
    echo.
    echo Prerequisites:
    echo   - Java 21 LTS
    echo   - Application built: build.bat
    echo   - JAR file: target\wex-transaction-1.0.0.jar
    echo.
    goto :eof

:check_prerequisites
    if not exist "%JAR_FILE%" (
        call :print_error "JAR file not found: %JAR_FILE%"
        echo Build the application first with: build.bat
        exit /b 1
    )
    
    java -version >nul 2>&1
    if errorlevel 1 (
        call :print_error "Java not found. Please install Java 21 LTS."
        exit /b 1
    )
    
    if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
    goto :eof

:start_app
    call :print_header "Starting %APP_NAME%"
    
    call :check_prerequisites
    
    call :print_info "Starting on port %PORT%..."
    
    REM Start application in new window
    start "%APP_NAME%" /min java ^
        -Dserver.port=%PORT% ^
        -jar "%JAR_FILE%"
    
    timeout /t 3 /nobreak
    
    call :print_success "Application started"
    echo.
    call :print_info "API endpoints:"
    call :print_url "Health:     http://localhost:%PORT%/api/actuator/health"
    call :print_url "Metrics:    http://localhost:%PORT%/api/actuator/metrics"
    call :print_url "Transactions: http://localhost:%PORT%/api/transactions"
    echo.
    call :print_info "View logs: type %LOG_FILE%"
    echo.
    goto :eof

:stop_app
    call :print_header "Stopping %APP_NAME%"
    
    REM Find and kill Java process running the JAR
    tasklist /fi "ImageName eq java.exe" /fi "WindowTitle eq *%APP_NAME%*" 2>nul | find /i "java.exe" >nul
    if !errorlevel! equ 0 (
        for /f "tokens=2" %%A in ('tasklist /fi "ImageName eq java.exe" 2^>nul ^| find /i "java"') do (
            call :print_info "Stopping process %%A..."
            taskkill /PID %%A /F >nul 2>&1
        )
        call :print_success "Application stopped"
    ) else (
        call :print_info "Application not running"
    )
    goto :eof

:restart_app
    call :print_header "Restarting %APP_NAME%"
    call :stop_app
    timeout /t 2 /nobreak
    call :start_app
    goto :eof

:check_status
    tasklist /fi "ImageName eq java.exe" 2>nul | find /i "java.exe" >nul
    if !errorlevel! equ 0 (
        call :print_success "Status: RUNNING"
        tasklist /fi "ImageName eq java.exe"
    ) else (
        call :print_info "Status: NOT RUNNING"
    )
    goto :eof

:view_logs
    if not exist "%LOG_FILE%" (
        call :print_error "Log file not found: %LOG_FILE%"
        exit /b 1
    )
    
    call :print_header "Application Logs"
    echo File: %LOG_FILE%
    echo.
    type "%LOG_FILE%"
    goto :eof

:start_debug
    call :print_header "Starting %APP_NAME% in Debug Mode"
    
    call :check_prerequisites
    
    call :print_info "Starting on port %PORT% with debug on port %DEBUG_PORT%..."
    echo.
    
    java ^
        -Dserver.port=%PORT% ^
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT% ^
        -jar "%JAR_FILE%"
    
    goto :eof

REM ============================================
REM Main Script
REM ============================================

if "%~1"=="" goto run_start
if "%~1"=="start" goto run_start
if "%~1"=="stop" goto run_stop
if "%~1"=="restart" goto run_restart
if "%~1"=="status" goto run_status
if "%~1"=="logs" goto run_logs
if "%~1"=="debug" goto run_debug
if "%~1"=="help" goto show_help
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help

call :print_error "Unknown option: %~1"
echo Use "run.bat help" for usage information
exit /b 1

:run_start
    call :start_app
    goto :end

:run_stop
    call :stop_app
    goto :end

:run_restart
    call :restart_app
    goto :end

:run_status
    call :check_status
    goto :end

:run_logs
    call :view_logs
    goto :end

:run_debug
    call :start_debug
    goto :end

:end
    endlocal
    exit /b 0
