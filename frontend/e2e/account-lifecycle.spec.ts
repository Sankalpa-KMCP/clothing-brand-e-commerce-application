import { test, expect, type Page } from '@playwright/test';

async function mockLogin(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        user: {
          id: 1,
          email: 'customer@example.com',
          firstName: 'Test',
          lastName: 'Customer',
          role: 'ROLE_CUSTOMER',
          emailVerified: true
        }
      })
    });
  });
}

async function signIn(page: Page, path = '/login') {
  await mockLogin(page);
  await page.goto(path);
  await page.getByPlaceholder('you@example.com').fill('customer@example.com');
  await page.getByPlaceholder('Enter your password').fill('SafePassword123!');
  await page.getByRole('button', { name: 'Sign In' }).click();
}

test('public payment returns render without mutating cart or orders', async ({ page }) => {
  const mutatingRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    const isApi = url.includes('/api/cart') || url.includes('/api/orders') || url.includes('/api/auth');
    if (isApi && request.method() !== 'GET') {
      mutatingRequests.push(`${request.method()} ${url}`);
    }
  });

  await page.goto('/payment/success?session_id=ignored');
  await expect(page).toHaveURL(/\/payment\/success/);
  await expect(page.getByRole('heading', { name: 'Payment received' })).toBeVisible();
  await expect(page.getByText('Payment confirmation may still be processing')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sign in to view orders' })).toHaveAttribute('href', '/login?returnTo=%2Forders');

  await page.goto('/payment/cancel?session_id=ignored');
  await expect(page).toHaveURL(/\/payment\/cancel/);
  await expect(page.getByRole('heading', { name: 'Payment cancelled' })).toBeVisible();
  await expect(page.getByText('Your bag remains available')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sign in to return to bag' })).toHaveAttribute('href', '/login?returnTo=%2Fcart');

  expect(mutatingRequests).toEqual([]);
});

test('login resumes only allowlisted internal returnTo destinations', async ({ page }) => {
  await page.route('**/api/orders**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true
      })
    });
  });

  await signIn(page, '/login?returnTo=%2Forders');
  await expect(page).toHaveURL(/\/orders$/);
  await expect(page.getByRole('heading', { name: 'My Orders' })).toBeVisible();
});

test('malicious returnTo falls back to the default profile destination', async ({ page }) => {
  await signIn(page, '/login?returnTo=https%253A%252F%252Fevil.example%252Forders');
  await expect(page).toHaveURL(/\/profile$/);
  await expect(page.getByRole('heading', { name: 'My Account' })).toBeVisible();
});

test('login keeps auth tokens out of localStorage and sessionStorage', async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/profile$/);

  const storageSnapshot = await page.evaluate(() => ({
    local: { ...localStorage },
    session: { ...sessionStorage }
  }));

  expect(JSON.stringify(storageSnapshot)).not.toContain('mock-access-token');
  expect(JSON.stringify(storageSnapshot)).not.toContain('mock-refresh-token');
});

test('registration verification-required UX does not expose tokens', async ({ page }) => {
  await page.route('**/api/auth/register', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        token: null,
        refreshToken: null,
        verificationRequired: true,
        user: {
          id: 2,
          email: 'new.customer@example.com',
          firstName: 'New',
          lastName: 'Customer',
          role: 'ROLE_CUSTOMER',
          emailVerified: false
        }
      })
    });
  });

  await page.goto('/register');
  await page.getByPlaceholder('John', { exact: true }).fill('New');
  await page.getByPlaceholder('Doe', { exact: true }).fill('Customer');
  await page.getByPlaceholder('john.doe@example.com').fill('new.customer@example.com');
  await page.getByPlaceholder('At least 8 characters').fill('SafePassword123!');
  await page.getByPlaceholder('Re-enter your password').fill('SafePassword123!');
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/verification-sent$/);
  await expect(page.getByRole('heading', { name: 'Verify your email' })).toBeVisible();
  await expect(page.getByPlaceholder('you@example.com')).toHaveValue('new.customer@example.com');
  expect(page.url()).not.toContain('token=');
});

test('verify and reset pages scrub URL tokens and show safe states', async ({ page }) => {
  await page.route('**/api/auth/verification/verify', async (route) => {
    expect(route.request().postDataJSON()).toEqual({ token: 'verify-token-123' });
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Email verified. You can now sign in.' })
    });
  });

  await page.goto('/verify-email?token=verify-token-123');
  await expect(page).toHaveURL(/\/verify-email$/);
  await expect(page.getByRole('heading', { name: 'Email verified' })).toBeVisible();

  await page.route('**/api/auth/reset-password', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      token: 'reset-token-456',
      password: 'NewPassword123!'
    });
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Password reset. You can now sign in.' })
    });
  });

  await page.goto('/reset-password?token=reset-token-456');
  await expect(page).toHaveURL(/\/reset-password$/);
  await page.getByPlaceholder('At least 8 characters').fill('NewPassword123!');
  await page.getByPlaceholder('Re-enter your password').fill('NewPassword123!');
  await page.getByRole('button', { name: 'Update password' }).click();
  await expect(page.getByText('Password reset. You can now sign in.')).toBeVisible();
});

test('forgot password and resend verification use generic responses', async ({ page }) => {
  await page.route('**/api/auth/forgot-password', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'If the account can receive password reset email, a message will be sent shortly.' })
    });
  });
  await page.goto('/forgot-password');
  await page.getByPlaceholder('you@example.com').fill('maybe@example.com');
  await page.getByRole('button', { name: 'Send reset instructions' }).click();
  await expect(page.getByText('If the account can receive password reset email')).toBeVisible();

  await page.route('**/api/auth/verification/resend', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'If the account can receive verification email, a message will be sent shortly.' })
    });
  });
  await page.goto('/verification-sent');
  await page.getByPlaceholder('you@example.com').fill('maybe@example.com');
  await page.getByRole('button', { name: 'Resend verification' }).click();
  await expect(page.getByText('If the account can receive verification email')).toBeVisible();
});
