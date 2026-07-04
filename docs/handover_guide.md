# Client Handover Guide (handover_guide.md)

This document provides the final client handover guide for the clothing brand e-commerce application. It separates the completed application capabilities from the client-owned staging and production deployment responsibilities.

---

## 1. Delivered Capabilities & Supported Scope

The following core modules are delivered, fully tested, and ready for deployment:

* **Product Catalog:** Category and product management, variants handling (size, color, stock tracking), and search capabilities.
* **Shopping Cart & Checkout:** In-memory cart operations, catalog item additions, variant validations, and order reservation locks (prevents double selling).
* **Payments (Stripe Integration):** Out-of-the-box Stripe Checkout integration.
  * *Status:* Fully implemented but **disabled by default**. Set up to transition securely to Stripe test or live mode upon configuration of secret API keys.
* **Authentication & Rate Limiting:** JWT-based stateless session management with HTTP-only cookie tokens, verification emails, password resets, and brute-force rate-limiting filters (using sliding-window in-memory buckets).
* **Observability & Logging:** Correlation-ID injection for trace propagation, liveness and readiness actuator probes, and protected Prometheus metrics scraping via `X-Metrics-Token` header.
* **Storage Abstraction:** Media upload API supporting local disk storage by default, with S3-compatible cloud object storage integration (tested for Cloudflare R2 and AWS S3).
* **Staging Harness:** Multi-container orchestrator (`compose.staging.yaml`) using a strict isolated network, separated database migration job (`db-migration`), and a hardened Nginx reverse proxy.

---

## 2. Local Developer Verification Commands

To verify the application quality gates locally, run the following commands:

### Backend Test Suite
Executes all 364 backend integration and unit tests:
```bash
cd backend
$env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="ecommerce_db"; $env:DB_USERNAME="postgres"; $env:DB_PASSWORD="devpassword123"; $env:JWT_SECRET="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; ./mvnw test
```

### Frontend Linting & Build
Runs code lint rules and compiles production assets:
```bash
cd frontend
npm run lint
npm run build
```

### Playwright E2E Tests
To run local browser automation integration tests:
1. Start the PostgreSQL container on port `5432` (`docker compose up -d`).
2. Boot the Spring Boot backend with admin bootstrap and details enabled:
   ```bash
   cd backend
   $env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="ecommerce_db"; $env:DB_USERNAME="postgres"; $env:DB_PASSWORD="devpassword123"; $env:JWT_SECRET="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; $env:ADMIN_BOOTSTRAP_ENABLED="true"; $env:ADMIN_EMAIL="admin@example.com"; $env:ADMIN_PASSWORD="Password123!"; $env:MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS="always"; ./mvnw spring-boot:run
   ```
3. Run the Playwright test runner:
   ```bash
   cd frontend
   $env:E2E_ADMIN_EMAIL="admin@example.com"; $env:E2E_ADMIN_PASSWORD="Password123!"; $env:VITE_STRIPE_CHECKOUT_ENABLED="false"; npx playwright test
   ```

---

## 3. Client-Owned Infrastructure & Accounts

The client is responsible for provisioning and maintaining the following accounts and services:

1. **Staging / Production Hosting:** Dedicated virtual machines (e.g., AWS EC2, DigitalOcean Droplet) or container platform (e.g., ECS, Kubernetes) with Docker Engine and Docker Compose v2.
2. **Domain, DNS & TLS Termination:** Registration of a public domain (e.g., `example.com`), DNS configuration mapping to the host, and TLS certificates (e.g., Let's Encrypt, Cloudflare) terminating at the load balancer or Nginx edge.
3. **Relational Database:** Staging and production PostgreSQL instances (managed cloud services like AWS RDS or isolated Compose containers).
4. **Stripe Account:** Stripe developer account for payment gateway credentials (`STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET`).
5. **SMTP Email Provider:** Email delivery service (e.g., SendGrid, Mailgun, AWS SES) for transactional user flows.
6. **Object Storage (S3/R2/CDN):** Durable cloud storage buckets (e.g., Cloudflare R2 or AWS S3) if cloud image hosting is enabled instead of local storage.
7. **Monitoring Destination:** Prometheus scraping daemon or observability aggregator (e.g., Grafana, Datadog) pointing to the protected metrics endpoints.

---

## 4. Secure Ownership Model

To safeguard credentials and preserve system security, the client and development teams must strictly adhere to the following ownership guidelines:

* **Billing & Account Owner:** The client must retain primary ownership of all third-party subscription accounts (Stripe, hosting, email, storage).
* **Least-Privilege Access:** Developers and DevOps engineers should only be granted secondary, restricted IAM roles. Never share root credentials.
* **Secret Isolation:** Environment secrets must **never** be checked into the source repository, shared in insecure communication channels (chat, email), or baked directly into Docker image layers.
* **Untracked Files:** Staging configurations are maintained strictly inside `.env.staging` which is git-ignored and populated on-target only.

---

## 5. Staging Deployment Boundaries

* **Harness Design:** Staging operates on a dedicated `compose.staging.yaml` definition.
* **Isolation:** The database and backend are closed to external traffic. Public access is permitted **only** through port `80` (Nginx/frontend), routing static HTML pages and proxying `/api` calls internally.
* **Flyway Job:** Separated database migration execution prevents racing conditions on application replica boot-ups.
* **Boundaries:** Direct host connection to backend port `8080` or DB port `5432` is prohibited. `app.auth-rate-limit.trust-forwarded-for=true` must be set **only** because Nginx handles edge validation and overrides client headers.

---

## 6. Production-Launch Prerequisites

Before initiating a production rollout, DevOps must verify:

1. All staging validations and smoke-testing checks pass.
2. Production-like JWT secret, database passwords, rate-limiting secrets, and metrics tokens are randomly generated and configured securely.
3. Production domain is mapped, and TLS termination is active with HTTPS-only redirection rules in Nginx.
4. CORS allowed origins (`CORS_ALLOWED_ORIGINS`) matches the production domain exactly and contains no `localhost` references.
5. Storage directories and file upload configurations are securely mapped with proper group permissions (`appuser` user context).

---

## 7. Rollbacks & Support Boundaries

### Support Scope
Development support covers codebase bug fixes, application feature updates, and default configuration templates. Staging and production infrastructure setups, cloud server provisioning, security group/firewall maintenance, and database backup routines are the responsibility of the client's DevOps provider.

### Container Rollback Procedure
If a container deployment exhibits errors:
1. Revert to the stable revision tag in the environment variables.
2. Restart the container group:
   ```bash
   docker compose -f compose.staging.yaml --env-file .env.staging down
   docker compose -f compose.staging.yaml --env-file .env.staging up -d
   ```

### Database Rollback
Flyway migrations are forward-only scripts. In the event of a corrupt schema change, DevOps must restore the database to the latest automated daily backup.

---

## 8. Known Limitations

* **No Real Deployment:** Staging deployment has only been validated via local Docker Compose config rendering. No VM provisioning or cloud infrastructure deployment was executed.
* **No Live Third-Party Gateway Tests:** Stripe checkout, SMTP mail routing, and S3-compatible cloud storage have only been validated using local mocks (Playwright) and local directory abstractions. No live client-owned test or production gateways have been invoked.
* **Controlled Storage Migration:** S3 image migration utility remains disabled and requires a separately approved staging dry-run validation prior to writing or modifying DB records.

---

## 9. Client Acceptance Checklist

- [ ] Repository checkout contains no tracked secrets or `.env` files.
- [ ] Staging Compose harness (`compose.staging.yaml`) compiles successfully.
- [ ] Hardened Nginx configuration (`nginx.staging.conf`) passes syntax check.
- [ ] Backend test suite reports 100% success rate (`364/364` tests passed).
- [ ] Frontend linter and production compile finish without errors.
- [ ] Playwright E2E local specs pass successfully.

---

## 10. Next Actions for Client / DevOps

1. **Configure Accounts:** Register the required third-party services (hosting VM, domain DNS, Stripe developer key, SMTP credentials).
2. **Setup Staging Environment:** Copy `.env.staging.example` to `.env.staging` on the host and fill in non-placeholder secrets.
3. **Execute Deployment:** Boot the isolated containers using:
   ```bash
   docker compose -f compose.staging.yaml --env-file .env.staging up -d
   ```
4. **Run Smoke Test:** Execute `./smoke-test-staging.sh <staging-url>` from an external path.
