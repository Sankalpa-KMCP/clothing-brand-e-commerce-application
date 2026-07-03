# E2E Browser Testing

This directory contains automated local browser E2E tests built with Playwright.

## Prerequisites

1. **Start PostgreSQL Database**:
   Make sure the database is running:
   ```powershell
   docker compose --env-file .env up -d
   ```

2. **Run Backend with Admin Bootstrap**:
   The backend must be running on port `8080` with the admin bootstrap enabled. For example, in your `.env` file, ensure you have:
   ```env
   ADMIN_BOOTSTRAP_ENABLED=true
   ADMIN_EMAIL=admin@example.com
   ADMIN_PASSWORD=your_secure_admin_password
   ```
   Start the backend with these environment variables loaded.

3. **Configure Playwright E2E Environment Variables**:
   You must set the E2E admin credentials as process-level environment variables so that Playwright can authenticate and seed test catalog data dynamically.

   **On Windows (PowerShell)**:
   ```powershell
   $env:E2E_ADMIN_EMAIL="admin@example.com"
   $env:E2E_ADMIN_PASSWORD="your_secure_admin_password"
   ```

   **On Linux/macOS**:
   ```bash
   export E2E_ADMIN_EMAIL="admin@example.com"
   export E2E_ADMIN_PASSWORD="your_secure_admin_password"
   ```

## Running Tests

From the `frontend/` directory, run:
```bash
npm run test:e2e
```

Playwright will automatically boot the Vite dev server on port `5173`, run the E2E checkout/order tests using local Chromium, and generate a HTML report.

## Directory Structure

* `e2e/protected-routes.spec.ts`: Tests that unauthenticated users are correctly redirected to `/login` when trying to access protected views.
* `e2e/checkout-orders.spec.ts`: Tests the complete customer checkout, registration, cart, address book, and order cancellation lifecycle.
* `e2e/stripe-checkout.spec.ts`: Uses mocked API responses to verify the Stripe-enabled checkout redirect flow without live Stripe availability.

To run only the mocked Stripe Checkout flow, enable the Vite build-time flag for that command:

```powershell
$env:VITE_STRIPE_CHECKOUT_ENABLED="true"; $env:PLAYWRIGHT_PORT="5174"; $env:PLAYWRIGHT_BASE_URL="http://localhost:5174"; npm run test:e2e -- e2e/stripe-checkout.spec.ts; Remove-Item Env:\VITE_STRIPE_CHECKOUT_ENABLED; Remove-Item Env:\PLAYWRIGHT_PORT; Remove-Item Env:\PLAYWRIGHT_BASE_URL
```

The mocked Stripe E2E test does not perform a live payment. For local test-mode checkout with Stripe CLI webhook forwarding, see `../../backend/README.md`.
