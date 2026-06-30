# Clothing Brand E-Commerce Backend

This is the Spring Boot backend foundation for the clothing brand e-commerce portfolio project.

## Requirements
* Java 21+
* Maven
* Docker (for running PostgreSQL locally)

## Local Development Setup

1. **Set up environment variables:**
   From the repository root (one level up from this directory), copy `.env.example` to `.env`:
   ```shell
   cp .env.example .env
   ```
   Set a secure password for `DB_PASSWORD` in the new `.env` file. (Do not commit this file!).

2. **Start the database:**
   Run the following command from the repository root to start PostgreSQL via Docker:
   ```shell
   docker-compose --env-file .env up -d
   # or
   docker compose --env-file .env up -d
   ```

3. **Run the Spring Boot application:**
   Inside this `backend` directory, start the application using the Maven Wrapper. If you're on Windows (PowerShell/CMD), you can pass the `.env` variables or simply set them in your environment first. Since Spring Boot can read from system environment variables, make sure they are loaded.

   *Windows (PowerShell):*
   A quick way to load `.env` and run in one step:
   ```powershell
   Get-Content ..\.env | ForEach-Object { 
       $name, $value = $_.Split('=', 2)
       [Environment]::SetEnvironmentVariable($name, $value)
   }
   .\mvnw spring-boot:run
   ```

   *Linux/macOS:*
   ```shell
   export $(grep -v '^#' ../.env | xargs) && ./mvnw spring-boot:run
   ```

4. **Verify Application is Running:**
   Once started, check the actuator health endpoint:
   ```
   http://localhost:8080/actuator/health
   ```
   It should return `{"status":"UP"}`.

## Database Migrations
We use Flyway for database migrations. Future schema changes will be placed in `src/main/resources/db/migration/`. Hibernate `ddl-auto` is set to `validate` to prevent unintended schema modifications.

## Admin Bootstrap
For local development and testing, you can explicitly opt-in to bootstrap an administrator account on startup.
Set the following environment variables in your `.env` file (never commit `.env`!):
```env
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_secure_password
```
This is disabled by default. If a customer already registered with this email, the bootstrap will safely back off and not elevate their privileges.
