# Task T1 - Environment & Boot Health Baseline

## 📋 Overview
Externalize configuration via environment variables, standardize logging, wire PostgreSQL cleanly, and prove health is UP with a live database connection.

## ✅ Completed Subtasks

### ST1.1 - Add dotenv loader & env surface
**Status:** ✅ COMPLETED

**Changes Made:**
- Added `java-dotenv` dependency (version 5.2.2) to `pom.xml`
- Updated `YanaBackendApplication.java` to load `.env` file before Spring Boot starts
- Created `.env.example` with all required environment variables
- Updated `.gitignore` to exclude `.env` and `logs/` directories

**Files Modified:**
- `pom.xml` - Added dotenv dependency
- `src/main/java/com/yanapure/app/YanaBackendApplication.java` - Added dotenv loading
- `.env.example` - Created template file
- `.gitignore` - Added environment file exclusions

### ST1.2 - Wire application.yml for env-driven DB & actuator
**Status:** ✅ COMPLETED

**Changes Made:**
- Updated `application.yml` to use environment variables with fallback defaults
- Added Flyway configuration for database migrations
- Configured actuator endpoints for health monitoring
- Added proper logging configuration
- Added Flyway dependencies to `pom.xml`

**Environment Variables Used:**
- `SERVER_PORT` (default: 8080)
- `DB_HOST` (default: localhost)
- `DB_PORT` (default: 5432)
- `DB_NAME` (default: yanapure)
- `DB_USER` (default: yanapure_app)
- `DB_PASSWORD` (no default - must be set)

**Files Modified:**
- `src/main/resources/application.yml` - Complete rewrite with env-driven config
- `pom.xml` - Added Flyway dependencies

### ST1.3 - Logback baseline w/ secret masking
**Status:** ✅ COMPLETED

**Changes Made:**
- Updated `logback-spring.xml` with file and console appenders
- Configured log rotation (10MB files, 1GB total, 14 days retention)
- Added proper log levels for different components
- Created `StartupLogger.java` for application startup logging

**Logging Features:**
- Console and file logging
- Automatic log rotation
- Secret masking (no sensitive data in logs)
- Proper log levels (INFO for root, OFF for Hibernate SQL)

**Files Modified:**
- `src/main/resources/logback-spring.xml` - Complete rewrite
- `src/main/java/com/yanapure/app/logging/StartupLogger.java` - Created

### ST1.4 - Local .env and first boot against Postgres
**Status:** ✅ COMPLETED

**Actions Performed:**
- Created local `.env` file with database credentials
- Successfully built the application with `mvn -q -DskipTests package`
- Started the application with `mvn spring-boot:run`
- Verified database connection and Flyway initialization

**Database Connection:**
- PostgreSQL 13.21 running on localhost:5432
- Database: yanapure
- User: yanapure_app
- Connection pool: HikariCP
- Flyway schema history table created successfully

### ST1.5 - Verify health endpoints
**Status:** ✅ COMPLETED

**Health Endpoint Tests:**

**Custom Health Endpoint (`/health`):**
```json
{
    "status": "UP"
}
```

**Actuator Health Endpoint (`/actuator/health`):**
```json
{
    "status": "UP"
}
```

**Verification Commands Used:**
```powershell
# Custom health endpoint
Invoke-RestMethod -Uri 'http://localhost:8080/health' -Method Get

# Actuator health endpoint  
Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -Method Get
```

## 📊 Application Startup Logs

Key startup events from `logs/app.log`:

```
17:48:11.889 [main] INFO  c.y.app.YanaBackendApplication - Starting YanaBackendApplication using Java 17.0.12
17:48:15.677 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port 8080 (http)
17:48:17.010 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Starting...
17:48:17.470 [main] INFO  com.zaxxer.hikari.pool.HikariPool - HikariPool-1 - Added connection
17:48:17.476 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Start completed.
17:48:17.588 [main] INFO  org.flywaydb.core.FlywayExecutor - Database: jdbc:postgresql://localhost:5432/yanapure (PostgreSQL 13.21)
17:48:18.176 [main] INFO  o.f.core.internal.command.DbMigrate - Schema "public" is up to date. No migration necessary.
```

## 🎯 Definition of Done - All Criteria Met

✅ **App boots with env-driven config** - No hardcoded secrets, all configuration externalized  
✅ **Health endpoints return UP** - Both custom and actuator endpoints responding correctly  
✅ **Logs are clean** - INFO level, file rotation working, no secrets exposed  
✅ **Reports created and indexed** - This report documents all changes and verification steps  

## 🚀 How to Reproduce

1. **Setup Environment:**
   ```bash
   cd yana-backend
   cp .env.example .env
   # Edit .env with your database credentials
   ```

2. **Build and Run:**
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

3. **Verify Health:**
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/actuator/health
   ```

4. **Check Logs:**
   ```bash
   tail -f logs/app.log
   ```

## 📁 Files Created/Modified

### New Files:
- `.env.example` - Environment variable template
- `src/main/java/com/yanapure/app/logging/StartupLogger.java` - Startup logging
- `task-reports/T1-Environment-Baseline.md` - This report

### Modified Files:
- `pom.xml` - Added dotenv and Flyway dependencies
- `src/main/java/com/yanapure/app/YanaBackendApplication.java` - Added dotenv loading
- `src/main/resources/application.yml` - Environment-driven configuration
- `src/main/resources/logback-spring.xml` - Enhanced logging configuration
- `.gitignore` - Added environment file exclusions

## 🔧 Dependencies Added

- `io.github.cdimascio:java-dotenv:5.2.2` - Environment variable loading
- `org.flywaydb:flyway-core` - Database migration framework
- `org.flywaydb:flyway-database-postgresql` - PostgreSQL support for Flyway

---

**Task T1 Status:** ✅ **COMPLETED SUCCESSFULLY**
