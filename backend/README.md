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

## Local Stripe Checkout Test Mode
Stripe Checkout is disabled by default in both the backend and frontend. Use only Stripe test-mode values locally; never use live credentials for this project milestone.

1. Configure backend placeholders in `.env`:
   ```env
   STRIPE_ENABLED=true
   STRIPE_SECRET_KEY=sk_test_your_test_key
   STRIPE_WEBHOOK_SECRET=whsec_your_cli_forwarding_secret
   STRIPE_CURRENCY=USD
   STRIPE_REDIRECT_BASE_URL=http://localhost:5173
   STRIPE_SESSION_TIMEOUT_SECONDS=1800
   ```

2. Configure the frontend separately in `frontend/.env`:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   VITE_STRIPE_CHECKOUT_ENABLED=true
   ```

3. Start the local stack:
   ```powershell
   docker compose --env-file .env up -d
   ```
   Start the backend with the `.env` variables loaded, then start the frontend from `frontend/` with `npm run dev`.

4. Forward Stripe CLI webhooks to the backend:
   ```shell
   stripe listen --forward-to localhost:8080/api/webhooks/stripe
   ```
   Copy the printed `whsec_...` value into `STRIPE_WEBHOOK_SECRET` and restart the backend.

5. Complete checkout using Stripe test card placeholders from Stripe's test documentation, such as `4242 4242 4242 4242` with any future expiry, CVC, and postal code.

To disable quickly, set either `STRIPE_ENABLED=false` on the backend or `VITE_STRIPE_CHECKOUT_ENABLED=false` on the frontend and restart the affected process.

## Local-to-S3 Image Migration Utility
A safe, idempotent migration task is provided to migrate existing local uploads to an S3-compatible bucket and rewrite the product/category database image URLs.

### Dry-run Command (Safe Analysis)
This command runs in analysis mode, performing zero database or S3 modifications. It prints a reconciliation summary and writes a detailed report to `migration-report.json`.
```powershell
$env:STORAGE_TYPE="s3"; `
$env:S3_ENDPOINT="https://<account-id>.r2.cloudflarestorage.com"; `
$env:S3_REGION="auto"; `
$env:S3_BUCKET_NAME="my-bucket"; `
$env:S3_ACCESS_KEY="mock"; `
$env:S3_SECRET_KEY="mock"; `
$env:S3_PUBLIC_URL_PREFIX="https://cdn.example.com/"; `
$env:MIGRATION_LOCAL_TO_S3_ENABLED="true"; `
$env:MIGRATION_LOCAL_TO_S3_CONFIRM="false"; `
$env:MIGRATION_LOCAL_TO_S3_DRY_RUN="true"; `
.\mvnw spring-boot:run
```

### Execution Command (Non-Dry-Run)
Ensure you back up the database before executing this command. Local source files are NOT automatically deleted post-migration.
```powershell
$env:STORAGE_TYPE="s3"; `
$env:S3_ENDPOINT="https://<account-id>.r2.cloudflarestorage.com"; `
$env:S3_REGION="auto"; `
$env:S3_BUCKET_NAME="my-bucket"; `
$env:S3_ACCESS_KEY="actual_access_key"; `
$env:S3_SECRET_KEY="actual_secret_key"; `
$env:S3_PUBLIC_URL_PREFIX="https://cdn.example.com/"; `
$env:MIGRATION_LOCAL_TO_S3_ENABLED="true"; `
$env:MIGRATION_LOCAL_TO_S3_CONFIRM="true"; `
$env:MIGRATION_LOCAL_TO_S3_DRY_RUN="false"; `
.\mvnw spring-boot:run
```

### Rollback Guidance
If you need to roll back to local disk storage:
1. Revert the database state using your database backup, or run a custom SQL update statement to revert the public URL prefixes back to your local serving prefix:
   ```sql
   UPDATE products SET image_url = replace(image_url, 'https://cdn.example.com/images/', 'http://localhost:8080/api/images/');
   UPDATE categories SET image_url = replace(image_url, 'https://cdn.example.com/images/', 'http://localhost:8080/api/images/');
   ```
2. Reset `STORAGE_TYPE=local` in your `.env` configuration file and restart the backend.
