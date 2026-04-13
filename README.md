# WEX Transaction Processing Application

A Java Spring Boot application for processing transactions, built with Java 21 and Maven.

## Project Overview

This is a modern Spring Boot application that demonstrates best practices for Java 21 development with Maven as the build tool.

**Technology Stack:**

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.2.4
- **Build Tool**: Maven
- **Database**: H2 (in-memory for development)

## Prerequisites

- Java 21 or later
- Maven 3.8.0 or later

## Project Structure

```
wex-transaction/
├── src/
│   ├── main/
│   │   ├── java/com/wex/transaction/
│   │   │   └── WexTransactionApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/wex/transaction/
├── pom.xml
└── README.md
```

## Quick Start

### Build the Project

```bash
mvn clean install
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Test the Application

```bash
mvn test
```

## API Endpoints

- `GET /api/` - Health check endpoint
- `GET /api/health` - Application health status

## Configuration

Configuration is managed through `src/main/resources/application.properties`:

- **Server Port**: 8080
- **Context Path**: /api
- **Database**: H2 (in-memory)
- **Logging Level**: INFO (root), DEBUG (application)

## Development Guidelines

- Use Spring dependency injection for component management
- Follow standard package structure: controller, service, repository, model
- Write unit tests for all business logic
- Use `application.properties` for externalized configuration
- Enable H2 console for development if needed (update `application.properties`)

## Build Commands

| Command                                      | Description           |
| -------------------------------------------- | --------------------- |
| `mvn clean install`                          | Build the project     |
| `mvn spring-boot:run`                        | Run the application   |
| `mvn test`                                   | Run unit tests        |
| `mvn clean package`                          | Create executable JAR |
| `java -jar target/wex-transaction-1.0.0.jar` | Run the JAR file      |

## IDE Setup

### IntelliJ IDEA

1. Open project from root directory
2. Right-click on `pom.xml` → "Add as Maven Project"
3. Configure JDK 21 in Project Settings

### VS Code

1. Install Extension Pack for Java
2. Open project folder
3. VS Code will auto-detect the project type

## License

This is a sample project for demonstration purposes.
