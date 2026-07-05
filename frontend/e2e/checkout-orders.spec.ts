import { test, expect, type Page } from '@playwright/test';

// Global variables to hold the dynamic E2E catalog data created via admin API
let seededProduct: { id: number; variantId: number; name: string } | null = null;
let categoryId: number | null = null;

// Share a single page context across all steps to preserve in-memory auth state
let sharedPage: Page;

// Client-side navigation helper using e2eNavigate to prevent page reload (which wipes in-memory auth token)
async function clientNavigate(page: Page, path: string) {
  await page.evaluate((targetPath) => {
    (window as any).e2eNavigate(targetPath);
  }, path);
  // Wait for the URL to update
  await page.waitForURL(new RegExp(path.replace(/:\w+/g, '\\d+')));
}

test.beforeAll(async ({ request, browser }) => {
  const rawBackendUrl = process.env.PLAYWRIGHT_BACKEND_URL || 'http://localhost:8080';
  const backendUrl = rawBackendUrl.trim().replace(/\/+$/, '');
  const adminEmail = process.env.E2E_ADMIN_EMAIL;
  const adminPassword = process.env.E2E_ADMIN_PASSWORD;

  if (!adminEmail || !adminPassword) {
    throw new Error(
      "E2E admin credentials are not set. Please define E2E_ADMIN_EMAIL and E2E_ADMIN_PASSWORD environment variables."
    );
  }

  // Preflight check: Actuator health check
  try {
    const health = await request.get(`${backendUrl}/actuator/health`);
    if (!health.ok()) {
      throw new Error(`Actuator health status: ${health.status()}`);
    }
    const data = await health.json();
    if (data.status !== 'UP') {
      throw new Error(`Backend status is not UP: ${JSON.stringify(data)}`);
    }
  } catch (error) {
    throw new Error(
      `Preflight check failed: Backend/PostgreSQL is unreachable at ${backendUrl}. ` +
      `Ensure backend is running and healthy.`
    );
  }

  // Admin login to retrieve token
  let adminToken = '';
  try {
    const loginRes = await request.post(`${backendUrl}/api/auth/login`, {
      data: { email: adminEmail, password: adminPassword }
    });
    if (!loginRes.ok()) {
      const text = await loginRes.text();
      throw new Error(`Status: ${loginRes.status()}, Body: ${text}`);
    }
    const data = await loginRes.json();
    adminToken = data.token;
  } catch (error) {
    throw new Error(`Admin login failed: ${error.message}. Double check E2E_ADMIN_EMAIL/PASSWORD credentials and check that AdminBootstrap was run.`);
  }

  const runId = Date.now();
  const categoryName = `Category_E2E_${runId}`;
  const productName = `Product_E2E_${runId}`;
  const sku = `SKU_E2E_${runId}`;

  // 1. Create Category
  const catRes = await request.post(`${backendUrl}/api/admin/categories`, {
    headers: { 'Authorization': `Bearer ${adminToken}` },
    data: {
      name: categoryName,
      description: 'E2E test category',
      imageUrl: 'http://example.com/cat.jpg'
    }
  });
  if (!catRes.ok()) {
    throw new Error(`Failed to create test category: ${await catRes.text()}`);
  }
  const categoryData = await catRes.json();
  categoryId = categoryData.id;

  // 2. Create Product
  const prodRes = await request.post(`${backendUrl}/api/admin/products`, {
    headers: { 'Authorization': `Bearer ${adminToken}` },
    data: {
      name: productName,
      description: 'E2E test product description',
      imageUrl: 'http://example.com/prod.jpg',
      categoryId: categoryId,
      active: true
    }
  });
  if (!prodRes.ok()) {
    throw new Error(`Failed to create test product: ${await prodRes.text()}`);
  }
  const productData = await prodRes.json();
  const productId = productData.id;

  // 3. Create Variant
  const varRes = await request.post(`${backendUrl}/api/admin/products/${productId}/variants`, {
    headers: { 'Authorization': `Bearer ${adminToken}` },
    data: {
      sku: sku,
      size: 'M',
      color: 'Black',
      price: 49.99
    }
  });
  if (!varRes.ok()) {
    throw new Error(`Failed to create variant: ${await varRes.text()}`);
  }
  const variantData = await varRes.json();
  const variantId = variantData.id;

  // 4. Adjust stock to 10
  const stockRes = await request.patch(`${backendUrl}/api/admin/products/${productId}/variants/${variantId}/stock`, {
    headers: { 'Authorization': `Bearer ${adminToken}` },
    data: {
      adjustment: 10
    }
  });
  if (!stockRes.ok()) {
    throw new Error(`Failed to adjust variant stock: ${await stockRes.text()}`);
  }

  seededProduct = {
    id: productId,
    variantId: variantId,
    name: productName
  };

  // Instantiate the shared page
  sharedPage = await browser.newPage();
});

test.afterAll(async () => {
  if (sharedPage) {
    await sharedPage.close();
  }
});

test('E2E Customer Checkout and Orders Journey', async () => {
  const page = sharedPage;
  const runId = Date.now();
  const customerEmail = `customer_e2e_${runId}@example.com`;
  const customerPassword = 'Password123!';
  let orderId: string | null = null;

  // --- Step 1: Customer Registration ---
  await page.goto('/register');
  await page.getByPlaceholder('John', { exact: true }).fill('E2ECustomer');
  await page.getByPlaceholder('Doe', { exact: true }).fill('Test');
  await page.getByPlaceholder('john.doe@example.com', { exact: true }).fill(customerEmail);
  await page.getByPlaceholder('At least 8 characters', { exact: true }).fill(customerPassword);
  await page.getByPlaceholder('Re-enter your password', { exact: true }).fill(customerPassword);
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/profile/);
  await expect(page.getByRole('link', { name: 'E2ECustomer' })).toBeVisible();

  // --- Step 2: Empty Cart Checkout Behavior ---
  await clientNavigate(page, '/checkout');
  await expect(page.locator('text=Your bag is empty')).toBeVisible();
  await expect(page.getByRole('button', { name: /Place Order|Complete Order/i })).toHaveCount(0);

  // --- Step 3: Product Variant Selection and Add-to-Cart ---
  expect(seededProduct).not.toBeNull();
  await clientNavigate(page, `/products/${seededProduct!.id}`);
  await expect(page.getByRole('heading', { name: seededProduct!.name })).toBeVisible();
  await page.getByRole('button', { name: 'Black' }).click();
  await page.getByRole('button', { name: 'M' }).click();
  await page.getByRole('button', { name: 'Add to Bag' }).click();

  await expect(page.locator('text=Added to bag successfully!')).toBeVisible();
  const bagLink = page.getByRole('link', { name: 'Bag' });
  await expect(bagLink).toContainText('1');

  // --- Step 4: Cart Item and Total Display ---
  await clientNavigate(page, '/cart');
  await expect(page.locator(`text=${seededProduct!.name}`)).toBeVisible();
  await expect(page.locator('text=Color: Black')).toBeVisible();
  await expect(page.locator('text=Size: M')).toBeVisible();
  await expect(page.getByRole('main').getByText('1', { exact: true })).toBeVisible();
  await expect(page.getByText('LKR 49.99 ea')).toBeVisible();

  // --- Step 5: Cart-with-no-Address Checkout Behavior ---
  await clientNavigate(page, '/checkout');
  await expect(page.locator('text=You don\'t have any saved addresses. Please add one to continue.')).toBeVisible();
  const placeOrderBtn = page.getByRole('button', { name: /Place Order|Complete Order/i });
  await expect(placeOrderBtn).toBeDisabled();

  // --- Step 6: Create Delivery Address ---
  await clientNavigate(page, '/addresses');
  await page.getByRole('button', { name: 'Add New Address' }).click();
  await page.getByPlaceholder('Home', { exact: true }).fill('Home');
  await page.getByPlaceholder('John Doe', { exact: true }).fill('E2E Recipient');
  await page.getByPlaceholder('+1 555-0199', { exact: true }).fill('+15550199');
  await page.getByPlaceholder('123 Main St', { exact: true }).fill('123 Main St');
  await page.getByPlaceholder('New York', { exact: true }).fill('New York');
  await page.getByPlaceholder('United States', { exact: true }).fill('United States');
  await page.getByRole('button', { name: 'Save Address' }).click();

  await expect(page.locator('text=E2E Recipient')).toBeVisible();
  await expect(page.locator('text=123 Main St')).toBeVisible();

  // --- Step 7: Checkout & Duplicate-Click Protection ---
  await clientNavigate(page, '/checkout');
  await expect(page.locator('text=E2E Recipient')).toBeVisible();

  let checkoutCalls = 0;
  await page.route('**/api/orders/checkout', async (route) => {
    checkoutCalls++;
    await route.continue();
  });

  const placeOrderSubmitBtn = page.getByRole('button', { name: /Place Order|Complete Order/i });
  await placeOrderSubmitBtn.click();
  try {
    await placeOrderSubmitBtn.click({ force: true, timeout: 300 });
  } catch (e) {
    // Ignore click failures once button becomes disabled
  }

  await expect(page).toHaveURL(/\/orders\/\d+/);
  expect(checkoutCalls).toBe(1);
  await expect(page.getByRole('link', { name: 'Bag' })).not.toContainText('1');

  const url = page.url();
  const match = url.match(/\/orders\/(\d+)/);
  expect(match).not.toBeNull();
  orderId = match![1];

  // --- Step 8: Order Detail Verification ---
  await clientNavigate(page, `/orders/${orderId}`);
  await expect(page.getByRole('heading', { name: `Order #${orderId}` })).toBeVisible();
  await expect(page.locator('h1').getByText('PLACED')).toBeVisible();
  await expect(page.locator(`text=${seededProduct!.name}`)).toBeVisible();
  await expect(page.getByText('Qty: 1 × LKR 49.99')).toBeVisible();
  await expect(page.locator('text=E2E Recipient')).toBeVisible();
  await expect(page.locator('text=123 Main St')).toBeVisible();
  await expect(page.locator('text=Order History')).toBeVisible();

  // --- Step 9: Order History Page Verification ---
  await clientNavigate(page, '/orders');
  await expect(page.locator(`text=#${orderId}`)).toBeVisible();
  await expect(page.locator('tbody tr').first().locator('text=PLACED')).toBeVisible();

  // --- Step 10: Order Cancellation ---
  await clientNavigate(page, `/orders/${orderId}`);
  const cancelBtn = page.getByRole('button', { name: 'Cancel Order' });
  await expect(cancelBtn).toBeVisible();
  
  page.once('dialog', dialog => dialog.accept());
  await cancelBtn.click();
  
  await expect(page.locator('h1').getByText('CANCELLED')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cancel Order' })).toHaveCount(0);

  // --- Step 11: Logout & State Cleared ---
  await page.getByRole('button', { name: 'Sign Out' }).click();
  await expect(page).toHaveURL(/\/login/);
  
  await page.goto('/orders');
  await expect(page).toHaveURL(/\/login/);
});
