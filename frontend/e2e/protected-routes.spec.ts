import { test, expect } from '@playwright/test';

/**
 * Protected-route redirect tests.
 *
 * These verify that unauthenticated visitors are immediately redirected to
 * /login by the client-side ProtectedRoute guard. They run against the Vite
 * dev server only — no backend, database, or user account is required because
 * the redirect is entirely a React-Router client-side decision based on the
 * in-memory authStore being empty.
 */

const PROTECTED_ROUTES = [
  { path: '/checkout', label: '/checkout' },
  { path: '/orders', label: '/orders' },
  { path: '/orders/1', label: '/orders/:id' },
];

for (const route of PROTECTED_ROUTES) {
  test.describe(`Protected route ${route.label}`, () => {

    test(`redirects signed-out visitor to /login`, async ({ page }) => {
      // Intercept any real API call to cart, address, or order endpoints and
      // fail the test if one is attempted — the redirect must happen before any
      // protected API request fires. Vite module requests (e.g.
      // /src/api/cartApi.ts) are excluded since they are source-file loads,
      // not backend REST calls.
      const forbiddenPatterns = ['/api/cart', '/api/addresses', '/api/orders'];
      const blockedRequests: string[] = [];

      page.on('request', (req) => {
        const url = req.url();
        const isSourceModule = url.includes('/src/') || /\.\w+s$/.test(url);
        if (!isSourceModule && forbiddenPatterns.some((p) => url.includes(p))) {
          blockedRequests.push(url);
        }
      });

      await page.goto(route.path);

      // The ProtectedRoute component should redirect to /login
      await expect(page).toHaveURL(/\/login/);

      // The login page should be visible
      await expect(page.locator('form')).toBeVisible();

      // No protected API requests should have been made
      expect(
        blockedRequests,
        `Expected no protected API requests, but saw: ${blockedRequests.join(', ')}`
      ).toHaveLength(0);
    });

    test(`does not render the protected page content`, async ({ page }) => {
      await page.goto(route.path);
      await expect(page).toHaveURL(/\/login/);

      // Verify the protected page's unique content is NOT present.
      // Checkout shows "Checkout", Orders shows "My Orders", OrderDetail
      // shows "Order #", and payment returns show payment status headings.
      const protectedHeadings = page.getByRole('heading', {
        name: /checkout|my orders|order #|payment received|payment cancelled/i,
      });
      await expect(protectedHeadings).toHaveCount(0);
    });

  });
}
