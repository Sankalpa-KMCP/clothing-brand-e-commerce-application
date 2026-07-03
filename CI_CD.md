# CI/CD and Release-Safety Guidelines

This document outlines the local equivalent commands, environment scopes, container safety rules, and validation checks used in the automated CI/CD pipeline.

---

## 1. Local Equivalent Commands

To validate the application quality gates locally, run the following commands:

### Backend Tests & Verification
Run the complete backend compiler, checkstyle rules, and unit/integration tests:
```bash
cd backend
./mvnw clean test
```

### Frontend Code Checks
Run lint rules and compile the production build:
```bash
cd frontend
npm ci
npm run lint
npm run build
```

### End-to-End Playwright Tests
To execute E2E tests:
1. Ensure the PostgreSQL container is active on port `5432` (`docker compose up -d`).
2. Boot the Spring Boot backend with the `admin.bootstrap.enabled=true` environment setting.
3. Execute Playwright in the frontend directory:
   ```bash
   cd frontend
   npx playwright install chromium
   # Run E2E tests
   $env:E2E_ADMIN_EMAIL="admin@example.com"; $env:E2E_ADMIN_PASSWORD="Password123!"; npx playwright test
   ```

---

## 2. Playwright Mock and Skip Policies
* **Stripe Checkout Tests:** Skip automatically by default in CI (`VITE_STRIPE_CHECKOUT_ENABLED=false`). These specs mock all gateway responses locally using Playwright's `page.route` intercepts.
* **Email verification and resets:** Executed via generic mocks without requesting live SMTP integration.

---

## 3. Container Context Security & Build Exclusions
Both the backend and frontend `.dockerignore` files are strictly configured to prevent secrets leaks:
* **Excluded files:** `.env`, `.env.*`, `uploads/`, `test-uploads/`, `*.log`, `node_modules/`, `target/`, and `dist/` directories are completely ignored by the Docker build context.
* **Non-root enforcement:** Containers run under the user `appuser` (Eclipse Temurin JRE alpine build base) to enforce least-privilege runtimes in Kubernetes/Fargate.

---

## 4. Release-Readiness Checks
Our CI workflow integrates a configuration validation suite:
- `ReleaseReadinessValidationTest` verifies that when features are configured for production, no default placeholders (like the default JWT secret or local CORS origins containing `localhost`) are active.
- To execute release readiness checks on local property overrides, pass:
  ```bash
  ./mvnw test -Dtest=ReleaseReadinessValidationTest
  ```
