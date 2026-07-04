# Staging Deployment Harness Runbook (STAGING_RUNBOOK.md)

This runbook documents the architecture, startup procedures, proxy trust assumptions, verification commands, and rollback procedures for the staging environment of the clothing brand e-commerce application.

---

## 1. Staging Runtime & Network Architecture

Staging is designed to be a **provider-neutral**, fully isolated, and secured Docker Compose topology. The network is segmented to ensure maximum security, following the principle of least privilege:

```mermaid
graph TD
    User([Public User / Client]) -->|Port 80| Nginx[Frontend / Nginx Reverse Proxy]
    subgraph Isolated Staging Network (staging-net)
        Nginx -->|Port 8080 /api| SpringBoot[Spring Boot Backend]
        SpringBoot -->|Port 5432| Postgres[(PostgreSQL Staging DB)]
        MigrationJob[One-Off DB Migration Job] -->|Port 5432| Postgres
    end
    classDef public fill:#f9f,stroke:#333,stroke-width:2px;
    classDef private fill:#bbf,stroke:#333,stroke-width:1px;
    class User public;
    class Nginx private;
    class SpringBoot private;
    class Postgres private;
    class MigrationJob private;
```

### Key Security & Network Characteristics:
* **Edge Routing:** The Nginx/frontend container is the **only** service with host-published ports (`STAGING_HTTP_PORT:-80`).
* **Isolation:** The PostgreSQL (`postgres-staging`) database and the Spring Boot (`backend`) application are kept on the private `staging-net` virtual network. They are not exposed to the host system or the public internet.
* **Flyway Separation:** DB migrations are entirely decoupled from normal backend startup. A temporary, one-off container (`db-migration`) executes the Flyway scripts first. The main backend containers boot up only after the migration completes successfully and start with Flyway disabled.

---

## 2. Hardened Reverse-Proxy & Forwarded-Header Trust Model

To enable secure client-IP tracking for rate-limiting while preventing IP spoofing behind the proxy, the following design is implemented:

### Forwarded-Header Hardening (Nginx):
Nginx acts as the edge reverse proxy. To prevent spoofing, Nginx **strips and overwrites** any client-supplied proxy headers. In `nginx.staging.conf`:
```nginx
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;
```
This guarantees that `X-Forwarded-For` contains **only** the trusted client IP as observed directly by Nginx.

### Backend Trust Flag:
The backend rate-limiting service extracts client IPs using `X-Forwarded-For` only when `app.auth-rate-limit.trust-forwarded-for=true` is enabled.
* **Compose Configuration:** This property is explicitly enabled via the environment in the staging `backend` Compose service definition:
  `app.auth-rate-limit.trust-forwarded-for: "true"`
* **Local / Dev Default:** Outside this controlled proxy topology, the property remains `false` by default, protecting local dev instances from spoofing attempts.

> [!WARNING]
> **DIRECT BACKEND EXPOSURE INVALIDATES THIS MODEL.**
> If the backend service is modified to publish port `8080` to the host/internet directly, malicious clients can bypass the reverse proxy, supply spoofed `X-Forwarded-For` headers directly, and bypass authentication rate-limiting.

---

## 3. Staging Startup Order & Procedures

All configurations are driven by an external environment file.

### Step 1: Prepare Environment File
1. Make a copy of `.env.staging.example` and name it `.env.staging` (untracked):
   ```bash
   cp .env.staging.example .env.staging
   ```
2. Populate the parameters in `.env.staging` with staging values (e.g., custom database credentials, 256-bit JWT secret, metrics scraping token).

### Step 2: Spin Up the Infrastructure
Start the staging services using the override file. The dependency chain automatically controls startup order:
1. `postgres-staging` starts first and performs healthchecks.
2. Once the DB is healthy, the one-off `db-migration` service boots, runs Flyway migrations, and terminates.
3. Once `db-migration` exits with success (exit code `0`), the `backend` boots with Flyway disabled.
4. Once `backend` reports healthy (Actuator `/health/readiness` returns `UP`), the `frontend` Nginx service launches.

```bash
docker compose -f compose.staging.yaml --env-file .env.staging up -d
```

### Step 3: Run One-off Migrations Manually (Optional)
To run migrations independently without booting the web application:
```bash
docker compose -f compose.staging.yaml --env-file .env.staging run --rm db-migration
```

---

## 4. Required Configuration Categories & Placeholders
The staging configurations are grouped into safe categories in `.env.staging.example`. Key properties requiring manual configuration by the environment owner include:

| Category | Key | Description | Staging Safe Placeholder |
| :--- | :--- | :--- | :--- |
| **Database** | `DB_PASSWORD` | PostgreSQL staging password | `change_me_to_a_secure_staging_password_12345` |
| **Security** | `JWT_SECRET` | 256-bit JWT secret token | `change_me_to_a_secure_staging_jwt_secret_key_at_least_32_characters_long` |
| **Abuse Protection**| `AUTH_RATE_LIMIT_HASH_SECRET` | Hash secret for rate-limiting buckets | `change_me_to_a_secure_staging_rate_limit_hash_secret_value` |
| **Metrics** | `METRICS_SECURITY_TOKEN` | Required header `X-Metrics-Token` | `change_me_to_a_secure_staging_metrics_scraping_token_at_least_16_chars` |
| **Routing** | `CORS_ALLOWED_ORIGINS` | Permitted browser origins | `https://staging.example.com` |

---

## 5. Smoke-Testing & Verification

A safe validation script is provided to automate checks. It does not invoke live third-party services (SMTP, Stripe, S3).

### Run Smoke Test Script
To execute the smoke test against the local staging environment:
```bash
# Set execute permissions (Unix-like)
chmod +x smoke-test-staging.sh

# Run the test against staging (defaults to http://localhost)
./smoke-test-staging.sh http://localhost
```

### Manual CLI Validation Commands
* **Render Compose Config:**
  ```bash
  docker compose -f compose.staging.yaml --env-file .env.staging config
  ```
* **Verify Public Endpoint is Accessible (Catalog):**
  ```bash
  curl -sI http://localhost/api/products | grep "HTTP/1.1 200"
  ```
* **Verify Prometheus is Blocked (Expected: 403):**
  ```bash
  curl -sI http://localhost/actuator/prometheus | grep "HTTP/1.1 403"
  ```
* **Verify Correlation ID Propagation:**
  ```bash
  curl -sI -H "X-Correlation-ID: test-id-999" http://localhost/actuator/health | grep -i "X-Correlation-ID"
  # Expected output: X-Correlation-ID: test-id-999
  ```

---

## 6. Rollback Procedures

### Container Rollback
If a staging deployment fails or displays erratic behavior, execute the following to rollback to a stable container build state:
1. Identify the previous stable image tag (e.g., `v1.2.0`).
2. Update the image reference or rebuild the service containers:
   ```bash
   # Rebuild to specific version
   docker compose -f compose.staging.yaml down
   # Deploy previous stable tags
   STAGING_HTTP_PORT=80 docker compose -f compose.staging.yaml --env-file .env.staging up -d
   ```

### Database Schema Rollback
Since Flyway migrations are one-way forward-only schemas, in case of a migration failure:
1. Shut down the backend services:
   ```bash
   docker compose -f compose.staging.yaml down
   ```
2. Restore the database from the latest staging backup (if automated backups are scheduled).
3. If no restore is available and schema changes must be manually reverted, run a manual migration revert query before re-running the migration job.

---

## 7. Explicit Owner Approvals (Feature Flags)

> [!CAUTION]
> **SEPARATE OWNER APPROVALS REQUIRED**
> The following staging settings can result in data mutation, charges, or compliance issues. Do NOT enable them without explicit approval from the project owner:
>
> 1. **Stripe Test Mode:** Enabling `STRIPE_ENABLED=true` binds the environment to live payment endpoints.
> 2. **Transactional SMTP Mailing:** Enabling `EMAIL_ENABLED=true` sends emails to real addresses. Only use sandbox SMTP settings.
> 3. **S3 Image Migration Write-Mode:** Enabling `MIGRATION_LOCAL_TO_S3_ENABLED=true` with `MIGRATION_LOCAL_TO_S3_DRY_RUN=false` and `MIGRATION_LOCAL_TO_S3_CONFIRM=true` starts rewriting backend product and category URLs to AWS S3/Cloudflare R2 storage.
