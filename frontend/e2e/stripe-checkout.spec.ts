import { test, expect, type Page } from '@playwright/test';

const stripeEnabled = process.env.VITE_STRIPE_CHECKOUT_ENABLED === 'true';
const checkoutRedirectUrl = `${process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'}/__stripe-hosted-checkout-mock`;

const cartResponse = {
  items: [
    {
      cartItemId: 101,
      productId: 201,
      productName: 'Mock Twill Jacket',
      imageUrl: 'http://example.com/jacket.jpg',
      variantId: 301,
      size: 'M',
      color: 'Black',
      quantity: 1,
      unitPrice: 49.99,
      lineTotal: 49.99,
      available: true
    }
  ],
  cartTotal: 49.99,
  totalQuantity: 1
};

const addressesResponse = [
  {
    id: 401,
    label: 'Home',
    recipientName: 'Stripe Customer',
    phoneNumber: '+15550199',
    addressLine1: '123 Test Lane',
    addressLine2: null,
    city: 'New York',
    region: 'NY',
    postalCode: '10001',
    country: 'United States',
    isDefault: true
  }
];

test.skip(!stripeEnabled, 'Stripe checkout E2E requires VITE_STRIPE_CHECKOUT_ENABLED=true');

async function clientNavigate(page: Page, path: string) {
  await page.evaluate((targetPath) => {
    (window as any).e2eNavigate(targetPath);
  }, path);
  await page.waitForURL(new RegExp(`${path.replace(/\//g, '\\/')}$`));
}

async function signInWithMocks(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        user: {
          id: 1,
          email: 'stripe.customer@example.com',
          firstName: 'Stripe',
          lastName: 'Customer',
          role: 'ROLE_CUSTOMER'
        }
      })
    });
  });

  await page.route('**/api/cart', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(cartResponse)
    });
  });

  await page.route('**/api/addresses', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(addressesResponse)
    });
  });

  await page.goto('/login');
  await page.getByPlaceholder('you@example.com').fill('stripe.customer@example.com');
  await page.getByPlaceholder('Enter your password').fill('Password123!');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page).toHaveURL(/\/profile/);
  await page.waitForFunction(() => typeof (window as any).e2eNavigate === 'function');
}

test('Stripe-enabled checkout reserves once and redirects to the returned URL', async ({ page }) => {
  await signInWithMocks(page);

  let reserveCalls = 0;
  const legacyCheckoutRequests: string[] = [];
  let releaseReservation: () => void = () => {};
  const reservationStarted = new Promise<void>((resolve) => {
    releaseReservation = resolve;
  });

  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().endsWith('/api/orders/checkout')) {
      legacyCheckoutRequests.push(request.url());
    }
  });

  await page.route('**/api/orders/checkout/reserve', async (route) => {
    reserveCalls++;
    expect(route.request().method()).toBe('POST');
    expect(route.request().postDataJSON()).toEqual({ addressId: 401 });
    await reservationStarted;
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        orderId: 901,
        stripeCheckoutUrl: checkoutRedirectUrl,
        reservationExpiresAt: '2026-07-02T13:00:00Z'
      })
    });
  });

  await clientNavigate(page, '/checkout');
  await expect(page.getByText('Stripe Customer')).toBeVisible();
  await expect(page.getByText('Your items are reserved temporarily', { exact: false })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Bag' })).toContainText('1');

  const payButton = page.getByRole('button', { name: 'Continue to Payment' });
  await payButton.click();
  const processingButton = page.getByRole('button', { name: 'Processing...' });
  await expect(processingButton).toBeDisabled();
  await expect(page.getByRole('link', { name: 'Bag' })).toContainText('1');

  try {
    await processingButton.click({ force: true, timeout: 300 });
  } catch {
    // The disabled button may reject the forced click after submission starts.
  }

  releaseReservation();

  await expect(page).toHaveURL(checkoutRedirectUrl);
  expect(reserveCalls).toBe(1);
  expect(legacyCheckoutRequests).toEqual([]);
});

test('Stripe reserve API failures render safe feedback and keep the cart visible', async ({ page }) => {
  await signInWithMocks(page);

  let reserveCalls = 0;
  await page.route('**/api/orders/checkout/reserve', async (route) => {
    reserveCalls++;
    await route.fulfill({
      status: 409,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 409,
        error: 'Conflict',
        message: 'Stripe payment is temporarily unavailable.',
        timestamp: '2026-07-02T13:00:00'
      })
    });
  });

  await clientNavigate(page, '/checkout');
  await expect(page.getByText('Stripe Customer')).toBeVisible();
  await page.getByRole('button', { name: 'Continue to Payment' }).click();

  await expect(page).toHaveURL(/\/checkout/);
  await expect(page.getByText('Stripe payment is temporarily unavailable.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Bag' })).toContainText('1');
  expect(reserveCalls).toBe(1);
});

test('payment return pages are informational and do not mutate orders or carts', async ({ page }) => {
  await signInWithMocks(page);

  const mutatingRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    const method = request.method();
    if (method !== 'GET' && (url.includes('/api/orders') || url.includes('/api/cart'))) {
      mutatingRequests.push(`${method} ${url}`);
    }
  });

  await clientNavigate(page, '/payment/success');
  await expect(page.getByRole('heading', { name: 'Payment received' })).toBeVisible();
  await expect(page.getByText('Payment confirmation may still be processing')).toBeVisible();

  await clientNavigate(page, '/payment/cancel');
  await expect(page.getByRole('heading', { name: 'Payment cancelled' })).toBeVisible();
  await expect(page.getByText('Your bag remains available')).toBeVisible();

  expect(mutatingRequests).toEqual([]);
});
