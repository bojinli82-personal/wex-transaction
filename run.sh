#!/bin/bash

# ============================================
# WEX Transaction - Run Script (Linux/macOS)
# ============================================
# Usage: ./run.sh [options]
#
# Options:
#   start              Start application (default)
#   stop               Stop application
#   restart            Restart application
#   status             Check status
#   logs               View logs
#   debug              Start in debug mode (port 5005)
#   help               Show this help message
#
# Environment Variables:
#   PORT               HTTP port (default: 8080)
#   DEBUG_PORT         Debug port (default: 5005)
# ============================================

set -e

# Configuration
APP_NAME="wex-transaction"
JAR_FILE="target/wex-transaction-1.0.0.jar"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
PID_FILE="${LOG_DIR}/${APP_NAME}.pid"
PORT="${PORT:-8080}"
DEBUG_PORT="${DEBUG_PORT:-5005}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Functions
print_header() {
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ SUCCESS: $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ INFO: $1${NC}"
}

print_url() {
    echo -e "${BLUE}→ $1${NC}"
}

show_help() {
    cat << EOF
WEX Transaction Application Launcher

Usage: ./run.sh [options]

Options:
  start (default)    Start application
  stop               Stop application gracefully
  restart            Restart application
  status             Check application status
  logs               View application logs (live)
  debug              Start in debug mode (port 5005)
  help               Show this help message

Examples:
  ./run.sh                       # Start application
  ./run.sh stop                  # Stop application
  ./run.sh restart               # Restart application
  ./run.sh logs                  # View logs
  ./run.sh debug                 # Debug mode

Environment Variables:
  PORT               HTTP port (default: 8080)
  DEBUG_PORT         Debug port (default: 5005)

URLs:
  Health Check       http://localhost:8080/api/actuator/health
  Metrics            http://localhost:8080/api/actuator/metrics
  Transactions       http://localhost:8080/api/transactions

Prerequisites:
  - Java 21 LTS
  - Application built: ./build.sh
  - JAR file: target/wex-transaction-1.0.0.jar

EOF
}

check_prerequisites() {
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found: $JAR_FILE"
        echo "Build the application first with: ./build.sh"
        exit 1
    fi
    
    if ! command -v java &> /dev/null; then
        print_error "Java not found. Please install Java 21 LTS."
        exit 1
    fi
    
    mkdir -p "$LOG_DIR"
}

start_app() {
    print_header "Starting $APP_NAME"
    
    check_prerequisites
    
    # Check if already running
    if [ -f "$PID_FILE" ]; then
        OLD_PID=$(cat "$PID_FILE")
        if kill -0 "$OLD_PID" 2>/dev/null; then
            print_error "Application already running (PID: $OLD_PID)"
            print_url "Health: http://localhost:$PORT/api/actuator/health"
            exit 1
        fi
    fi
    
    # Start application
    print_info "Starting on port $PORT..."
    nohup java \
        -Dserver.port=$PORT \
        -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    
    NEW_PID=$!
    echo "$NEW_PID" > "$PID_FILE"
    
    # Wait for startup
    sleep 3
    
    # Verify startup
    if kill -0 "$NEW_PID" 2>/dev/null; then
        print_success "Application started (PID: $NEW_PID)"
        echo
        print_info "API endpoints:"
        print_url "Health:     http://localhost:$PORT/api/actuator/health"
        print_url "Metrics:    http://localhost:$PORT/api/actuator/metrics"
        print_url "Transactions: http://localhost:$PORT/api/transactions"
        echo
        print_info "View logs: tail -f $LOG_FILE"
    else
        print_error "Application failed to start"
        if [ -f "$LOG_FILE" ]; then
            echo "Last 20 log lines:"
            tail -20 "$LOG_FILE"
        fi
        exit 1
    fi
}

stop_app() {
    print_header "Stopping $APP_NAME"
    
    if [ ! -f "$PID_FILE" ]; then
        print_info "Application not running"
        return
    fi
    
    PID=$(cat "$PID_FILE")
    
    if kill -0 "$PID" 2>/dev/null; then
        print_info "Sending SIGTERM to process $PID..."
        kill -TERM "$PID"
        
        # Wait for graceful shutdown (max 30 seconds)
        for i in {1..30}; do
            if ! kill -0 "$PID" 2>/dev/null; then
                print_success "Application stopped"
                rm -f "$PID_FILE"
                return
            fi
            sleep 1
        done
        
        print_info "Forcing kill..."
        kill -9 "$PID" 2>/dev/null || true
    fi
    
    rm -f "$PID_FILE"
    print_success "Application stopped"
}

restart_app() {
    print_header "Restarting $APP_NAME"
    stop_app
    sleep 2
    start_app
}

check_status() {
    if [ ! -f "$PID_FILE" ]; then
        print_info "Status: NOT RUNNING"
        return
    fi
    
    PID=$(cat "$PID_FILE")
    
    if kill -0 "$PID" 2>/dev/null; then
        print_success "Status: RUNNING (PID: $PID)"
        
        # Try to get health status
        if command -v curl &> /dev/null; then
            HEALTH=$(curl -s http://localhost:$PORT/api/actuator/health | grep -o '"status":"[^"]*"')
            if [ -n "$HEALTH" ]; then
                print_info "Health: $HEALTH"
            fi
        fi
    else
        print_info "Status: NOT RUNNING (stale PID: $PID)"
        rm -f "$PID_FILE"
    fi
}

view_logs() {
    if [ ! -f "$LOG_FILE" ]; then
        print_error "Log file not found: $LOG_FILE"
        return 1
    fi
    
    print_header "Application Logs (Ctrl+C to exit)"
    echo "File: $LOG_FILE"
    echo
    tail -f "$LOG_FILE"
}

start_debug() {
    print_header "Starting $APP_NAME in Debug Mode"
    
    check_prerequisites
    
    # Check if already running
    if [ -f "$PID_FILE" ]; then
        OLD_PID=$(cat "$PID_FILE")
        if kill -0 "$OLD_PID" 2>/dev/null; then
            print_error "Application already running (PID: $OLD_PID)"
            exit 1
        fi
    fi
    
    # Start in debug mode
    print_info "Starting on port $PORT with debug on port $DEBUG_PORT..."
    java \
        -Dserver.port=$PORT \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT \
        -jar "$JAR_FILE"
}

# Main script
case "${1:-start}" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        restart_app
        ;;
    status)
        check_status
        ;;
    logs)
        view_logs
        ;;
    debug)
        start_debug
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use './run.sh help' for usage information"
        exit 1
        ;;
esac
