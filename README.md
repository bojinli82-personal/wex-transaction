# WEX Transaction Management Application

> A robust Java 21 Spring Boot application for managing purchase transactions with real-time currency conversion using US Treasury exchange rates.

## Overview

The WEX Transaction Management Application provides a REST API to:

- **Create** purchase transactions with USD amounts
- **Retrieve** transactions and convert amounts to any currency using live Treasury exchange rates
- **Cache** exchange rates for 24 hours to minimize external API calls
- **Retry** failed API calls with exponential backoff
- **Validate** all inputs with comprehensive error handling

## Features

✅ **REST API Endpoints**

- `POST /api/transactions` - Create new transaction (201 Created)
- `GET /api/transactions/{id}?currency={code}` - Get transaction with currency conversion (200 OK)

✅ **Technology Stack**

- Java 21 LTS
- Spring Boot 3.2.0
- H2 In-Memory Database
- Maven 3.8+
- JUnit 5 Jupiter
- Mockito for testing

✅ **Production Ready**

- 67 comprehensive tests (29 service + 25 integration + 13 controller)
- Request validation with Jakarta Validation
- Exception handling with meaningful HTTP status codes
- Structured logging with file rotation
- Graceful error responses
- Exchange rate caching with TTL
- Retry logic with exponential backoff
- Connection pooling and batch processing

✅ **API Features**

- Treasury Exchange Rate API integration
- Automatic retry mechanism (5 attempts)
- 24-hour exchange rate caching
- 30-second API timeout
- Comprehensive error handling (404, 422, 400)
- Request validation (size, range, format)

## Prerequisites

### Required

- **Java 21 LTS** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Git** - [Download](https://git-scm.com/downloads)

### Verify Installation

```bash
java -version
mvn -version
git --version
```

## Project Structure

```
wex-transaction/
├── src/
│   ├── main/
│   │   ├── java/com/wex/transaction/
│   │   │   ├── WexTransactionApplication.java       # Main Spring Boot class
│   │   │   ├── controller/
│   │   │   │   └── PurchaseTransactionController.java
│   │   │   ├── service/
│   │   │   │   └── PurchaseTransactionService.java
│   │   │   ├── model/
│   │   │   │   └── PurchaseTransaction.java
│   │   │   ├── dto/
│   │   │   │   └── CreatePurchaseTransactionRequest.java
│   │   │   └── external/
│   │   │       └── TreasuryExchangeRateClient.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/wex/transaction/
│           ├── PurchaseTransactionControllerIntegrationTest.java
│           ├── PurchaseTransactionServiceTest.java
│           └── TreasuryExchangeRateClientTest.java
├── pom.xml                                           # Maven configuration
├── build.sh / build.bat                              # Build scripts
├── run.sh / run.bat                                  # Run scripts
└── README.md                                         # This file
```

## Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd wex-transaction
```

### 2. Build Application

#### Linux/macOS

```bash
./build.sh
```

#### Windows

```cmd
build.bat
```

#### Manual (All platforms)

```bash
mvn clean package
```

### 3. Run Application

#### Linux/macOS

```bash
./run.sh
```

#### Windows

```cmd
run.bat
```

#### Manual (All platforms)

```bash
java -jar target/wex-transaction-1.0.0.jar
```

### 4. Test the API

```bash
# Create a transaction
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Office supplies",
    "transactionDate": "2024-01-15",
    "amountUsd": 100.50
  }'

# Response (201 Created)
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "description": "Office supplies",
  "transactionDate": "2024-01-15",
  "amountUsd": 100.50,
  "message": "Transaction created successfully"
}

# Get transaction with currency conversion
curl "http://localhost:8080/api/transactions/a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6?currency=GBP"

# Response (200 OK)
{
  "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "description": "Office supplies",
  "transactionDate": "2024-01-15",
  "amountUsd": 100.50,
  "convertedAmount": 81.75,
  "currency": "GBP",
  "exchangeRate": 0.8124,
  "conversionDate": "2024-01-15"
}
```

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=PurchaseTransactionServiceTest
mvn test -Dtest=PurchaseTransactionControllerIntegrationTest
mvn test -Dtest=TreasuryExchangeRateClientTest
```

### Run with Coverage

```bash
mvn test jacoco:report
# View report: target/site/jacoco/index.html
```

### Test Statistics

- **Total Tests:** 67
- **Service Unit Tests:** 29 (business logic validation)
- **Treasury Client Tests:** 25 (API integration)
- **Controller Integration Tests:** 13 (HTTP contracts)
- **Success Rate:** 100% ✓

## API Documentation

### POST /api/transactions - Create Transaction

**Request:**

```json
{
  "description": "string (1-50 chars, required)",
  "transactionDate": "YYYY-MM-DD (required, not future)",
  "amountUsd": "decimal (required, >= 0.01)"
}
```

**Response (201):**

```json
{
  "id": "UUID",
  "description": "string",
  "transactionDate": "YYYY-MM-DD",
  "amountUsd": "decimal",
  "message": "Transaction created successfully"
}
```

**Error Responses:**

- `400 Bad Request` - Invalid input (missing/invalid fields)
- `422 Unprocessable Entity` - Business logic error

### GET /api/transactions/{id} - Get Transaction with Conversion

**Query Parameters:**

- `currency` (required) - ISO 4217 currency code (e.g., GBP, JPY, EUR)

**Response (200):**

```json
{
  "id": "UUID",
  "description": "string",
  "transactionDate": "YYYY-MM-DD",
  "amountUsd": "decimal",
  "convertedAmount": "decimal",
  "currency": "string",
  "exchangeRate": "decimal",
  "conversionDate": "YYYY-MM-DD"
}
```

**Error Responses:**

- `404 Not Found` - Transaction not found
- `422 Unprocessable Entity` - Currency not available for conversion

### GET /api/actuator/health - Health Check

**Response (200):**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### GET /api/actuator/metrics - Application Metrics

**Response (200):**

```json
{
  "names": [
    "jvm.memory.used",
    "jvm.threads.live",
    "http.server.requests",
    ...
  ]
}
```

## Configuration

### Application Properties

The application reads configuration from `src/main/resources/application.properties`:

| Property                          | Default      | Description          |
| --------------------------------- | ------------ | -------------------- |
| `server.port`                     | 8080         | HTTP port            |
| `server.servlet.context-path`     | /api         | API base path        |
| `spring.datasource.url`           | H2 in-memory | Database URL         |
| `treasury.api.timeout-seconds`    | 30           | API timeout          |
| `treasury.api.max-retries`        | 5            | Retry attempts       |
| `cache.exchange-rate.ttl-minutes` | 1440         | Cache duration (24h) |

### Environment Variables

To override properties at runtime:

```bash
# Set custom port
java -Dserver.port=9090 -jar target/wex-transaction-1.0.0.jar

# Set custom database
java -Dspring.datasource.url=jdbc:h2:file:./wex_db -jar target/wex-transaction-1.0.0.jar
```

## Building & Packaging

### Development Build

```bash
mvn clean package
```

### Fast Build (Skip Tests)

```bash
mvn clean package -DskipTests
```

### Build with Specific Java Version

```bash
mvn clean package -Djavac.version=21
```

### Build Output

```
target/
├── wex-transaction-1.0.0.jar              # Executable JAR (Spring Boot)
├── wex-transaction-1.0.0.jar.original     # Original JAR (Maven)
└── classes/                               # Compiled classes
```

## Running the Application

### Standard Startup

```bash
java -jar target/wex-transaction-1.0.0.jar
```

### With Custom Port

```bash
java -Dserver.port=9090 -jar target/wex-transaction-1.0.0.jar
```

### With JVM Tuning (Production)

```bash
java -server \
  -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar target/wex-transaction-1.0.0.jar
```

### View Logs

```bash
tail -f logs/wex-transaction.log
```

## Troubleshooting

### Port Already in Use

```bash
# On Linux/macOS: Find process using port 8080
lsof -i :8080

# On Windows: Find process using port 8080
netstat -ano | findstr :8080

# Kill process and retry
kill -9 <PID>  # Linux/macOS
taskkill /PID <PID> /F  # Windows
```

### Build Failures

**Error: `java: invalid version: 21`**

- Ensure Java 21 is installed and `JAVA_HOME` is set correctly
- Verify: `java -version`

**Error: `mvn: command not found`**

- Maven is not installed or not in PATH
- Download Maven 3.8+ from [maven.apache.org](https://maven.apache.org)

**Error: `Cannot connect to Treasury API`**

- Check internet connection
- Verify Treasury API is accessible: `curl https://api.fiscaldata.treasury.gov`

### Test Failures

All tests should pass on clean checkout:

```bash
mvn clean test
```

If tests fail:

1. Check Java version: `java -version` (must be 21+)
2. Clear cache: `mvn clean`
3. Rebuild: `mvn package`

## Performance Considerations

### Memory Usage

- **Minimum:** 512MB heap (`-Xms512m`)
- **Maximum:** 2GB heap (`-Xmx2g`)
- **Recommended:** Start with 512MB, monitor and adjust

### Connection Pooling

- **Default:** 10 connections (HikariCP)
- **Production:** 20 connections
- **Configure:** `spring.datasource.hikari.maximum-pool-size`

### Exchange Rate Caching

- **TTL:** 24 hours (1440 minutes)
- **Max Size:** 5000 entries
- **Hit Ratio:** >80% for repeat conversions

---

**Author**: Bo  
**Version**: 1.0.0  
**License**: Proprietary  
**Last Updated**: 2026
