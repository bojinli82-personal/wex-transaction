#!/bin/bash

# ============================================
# WEX Transaction - Build Script (Linux/macOS)
# ============================================
# Usage: ./build.sh [options]
#
# Options:
#   clean             Clean build (default)
#   skip-tests        Skip unit tests
#   skip-resources    Skip resource processing
#   help              Show this help message
# ============================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
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

show_help() {
    cat << EOF
WEX Transaction Build Script

Usage: ./build.sh [options]

Options:
  clean (default)    Clean build with tests
  skip-tests         Build without running tests
  skip-resources     Skip resource processing
  help               Show this help message

Examples:
  ./build.sh                 # Standard clean build with tests
  ./build.sh skip-tests      # Fast build without tests
  ./build.sh clean           # Same as default

Environment:
  JAVA_HOME          Path to Java 21 installation (auto-detected)
  MVN_OPTS           Maven options (optional)

Output:
  target/wex-transaction-1.0.0.jar    Executable JAR file

EOF
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java not found. Please install Java 21 LTS."
        echo "Download: https://www.oracle.com/java/technologies/downloads/#java21"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    print_info "Java version: $JAVA_VERSION"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven not found. Please install Maven 3.8+."
        echo "Download: https://maven.apache.org/download.cgi"
        exit 1
    fi
    
    MVN_VERSION=$(mvn -v | head -1)
    print_info "Maven version: $MVN_VERSION"
    
    # Check Git (optional)
    if command -v git &> /dev/null; then
        GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
        GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        print_info "Git branch: $GIT_BRANCH ($GIT_COMMIT)"
    fi
    
    print_success "All prerequisites met"
    echo
}

build_project() {
    print_header "Building WEX Transaction Application"
    
    # Build command
    BUILD_CMD="mvn clean package"
    
    # Add options
    case "$1" in
        skip-tests)
            print_info "Skipping unit tests"
            BUILD_CMD="$BUILD_CMD -DskipTests"
            ;;
        skip-resources)
            print_info "Skipping resource processing"
            BUILD_CMD="$BUILD_CMD -Dskip.npm"
            ;;
    esac
    
    echo -e "${YELLOW}Running: $BUILD_CMD${NC}"
    echo
    
    # Execute build
    if eval "$BUILD_CMD"; then
        print_success "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
    echo
}

verify_build() {
    print_header "Verifying Build Output"
    
    JAR_FILE="target/wex-transaction-1.0.0.jar"
    
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found: $JAR_FILE"
        exit 1
    fi
    
    # Get file size
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "JAR created: $JAR_FILE ($JAR_SIZE)"
    
    # Show manifest
    print_info "Main class: $(unzip -p "$JAR_FILE" META-INF/MANIFEST.MF | grep Main-Class | cut -d' ' -f2)"
    
    # Show implementation version
    IMPL_VERSION=$(unzip -p "$JAR_FILE" META-INF/MANIFEST.MF | grep Implementation-Version | cut -d' ' -f2)
    print_info "Version: $IMPL_VERSION"
    
    echo
}

show_next_steps() {
    print_header "Next Steps"
    
    echo "1. Run the application:"
    echo -e "   ${GREEN}./run.sh${NC}"
    echo
    echo "2. Or run manually:"
    echo -e "   ${GREEN}java -jar target/wex-transaction-1.0.0.jar${NC}"
    echo
    echo "3. Test the API:"
    echo -e "   ${GREEN}curl http://localhost:8080/api/actuator/health${NC}"
    echo
}

# Main script
case "${1:-clean}" in
    clean)
        check_prerequisites
        build_project
        verify_build
        show_next_steps
        ;;
    skip-tests)
        check_prerequisites
        build_project "skip-tests"
        verify_build
        show_next_steps
        ;;
    skip-resources)
        check_prerequisites
        build_project "skip-resources"
        verify_build
        show_next_steps
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use './build.sh help' for usage information"
        exit 1
        ;;
esac

print_success "Build process complete!"
