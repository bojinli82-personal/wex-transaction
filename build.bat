@echo off
REM ============================================
REM WEX Transaction - Build Script (Windows)
REM ============================================
REM Usage: build.bat [options]
REM
REM Options:
REM   clean             Clean build (default)
REM   skip-tests        Skip unit tests
REM   skip-resources    Skip resource processing
REM   help              Show this help message
REM ============================================

setlocal enabledelayedexpansion

REM Colors using ANSI escape codes (Windows 10+)
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
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
    echo %GREEN%^^! SUCCESS: %~1%NC%
    goto :eof

:print_info
    echo %YELLOW%i INFO: %~1%NC%
    goto :eof

:show_help
    cls
    echo WEX Transaction Build Script
    echo.
    echo Usage: build.bat [options]
    echo.
    echo Options:
    echo   clean (default)    Clean build with tests
    echo   skip-tests         Build without running tests
    echo   skip-resources     Skip resource processing
    echo   help               Show this help message
    echo.
    echo Examples:
    echo   build.bat                 # Standard clean build with tests
    echo   build.bat skip-tests      # Fast build without tests
    echo   build.bat clean           # Same as default
    echo.
    echo Environment:
    echo   JAVA_HOME          Path to Java 21 installation (auto-detected)
    echo   M2_HOME            Path to Maven installation (optional)
    echo.
    echo Output:
    echo   target\wex-transaction-1.0.0.jar    Executable JAR file
    echo.
    goto :eof

:check_prerequisites
    call :print_header "Checking Prerequisites"
    
    REM Check Java
    java -version >nul 2>&1
    if errorlevel 1 (
        call :print_error "Java not found. Please install Java 21 LTS."
        echo Download: https://www.oracle.com/java/technologies/downloads/#java21
        exit /b 1
    )
    
    for /f tokens^=2 %%j in ('java -version 2^>^&1 ^| find /i "version"') do set JAVA_VER=%%j
    call :print_info "Java version: %JAVA_VER%"
    
    REM Check Maven
    mvn -v >nul 2>&1
    if errorlevel 1 (
        call :print_error "Maven not found. Please install Maven 3.8+."
        echo Download: https://maven.apache.org/download.cgi
        exit /b 1
    )
    
    for /f "tokens=*" %%m in ('mvn -v 2^>^&1 ^| findstr /i "Apache"') do set MVN_VER=%%m
    call :print_info "Maven version: %MVN_VER%"
    
    call :print_success "All prerequisites met"
    goto :eof

:build_project
    call :print_header "Building WEX Transaction Application"
    
    set "BUILD_CMD=mvn clean package"
    
    if "%~1"=="skip-tests" (
        call :print_info "Skipping unit tests"
        set "BUILD_CMD=!BUILD_CMD! -DskipTests"
    )
    
    if "%~1"=="skip-resources" (
        call :print_info "Skipping resource processing"
        set "BUILD_CMD=!BUILD_CMD! -Dskip.npm"
    )
    
    echo %YELLOW%Running: !BUILD_CMD!%NC%
    echo.
    
    %BUILD_CMD%
    if errorlevel 1 (
        call :print_error "Build failed"
        exit /b 1
    )
    
    call :print_success "Build completed successfully"
    echo.
    goto :eof

:verify_build
    call :print_header "Verifying Build Output"
    
    set "JAR_FILE=target\wex-transaction-1.0.0.jar"
    
    if not exist "%JAR_FILE%" (
        call :print_error "JAR file not found: %JAR_FILE%"
        exit /b 1
    )
    
    REM Get file size
    for %%A in ("%JAR_FILE%") do set "JAR_SIZE=%%~zA bytes"
    call :print_success "JAR created: %JAR_FILE% (%JAR_SIZE%)"
    
    call :print_info "Version: 1.0.0"
    echo.
    goto :eof

:show_next_steps
    call :print_header "Next Steps"
    
    echo 1. Run the application:
    echo    %GREEN%run.bat%NC%
    echo.
    echo 2. Or run manually:
    echo    %GREEN%java -jar target\wex-transaction-1.0.0.jar%NC%
    echo.
    echo 3. Test the API:
    echo    %GREEN%curl http://localhost:8080/api/actuator/health%NC%
    echo.
    goto :eof

REM ============================================
REM Main Script
REM ============================================

if "%~1"=="" goto build_clean
if "%~1"=="clean" goto build_clean
if "%~1"=="skip-tests" goto build_skip_tests
if "%~1"=="skip-resources" goto build_skip_resources
if "%~1"=="help" goto show_help
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help

call :print_error "Unknown option: %~1"
echo Use "build.bat help" for usage information
exit /b 1

:build_clean
    call :check_prerequisites
    call :build_project
    call :verify_build
    call :show_next_steps
    call :print_success "Build process complete!"
    goto :end

:build_skip_tests
    call :check_prerequisites
    call :build_project skip-tests
    call :verify_build
    call :show_next_steps
    call :print_success "Build process complete!"
    goto :end

:build_skip_resources
    call :check_prerequisites
    call :build_project skip-resources
    call :verify_build
    call :show_next_steps
    call :print_success "Build process complete!"
    goto :end

:end
    endlocal
    exit /b 0
