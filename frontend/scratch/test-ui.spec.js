import { test, expect } from '@playwright/test';

test('capture vehicle modal with flag country code and year calendar picker', async ({ page }) => {
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/auth/me') {
      return route.fulfill({
        json: {
          role: 'ROLE_MANAGER',
          user: { id: 1, username: 'manager', fullName: 'Station Manager', email: 'manager@station.com' },
        },
      });
    }
    if (path === '/api/workshop/vehicles/registry') {
      return route.fulfill({ json: { vehicles: [] } });
    }
    if (path === '/api/billing/customer-accounts') {
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ json: {} });
  });

  await page.goto('/dashboard/vehicles');
  await page.getByRole('button', { name: 'Register vehicle', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();

  // Country code dropdown shows flag + calling code, NOT country name
  const countryTrigger = page.getByRole('combobox', { name: /Country code for/ });
  await expect(countryTrigger).toBeVisible();
  await expect(countryTrigger).toContainText('+94');

  // Capture screenshot of vehicle modal
  await page.screenshot({ path: 'scratch/vehicle-modal.png', fullPage: true });

  // Open the year calendar picker
  await page.getByRole('button', { name: /Manufacture year/i }).click();
  await expect(page.getByRole('dialog', { name: 'Year calendar' })).toBeVisible();

  // Capture screenshot with Year Calendar Picker open
  await page.screenshot({ path: 'scratch/year-calendar-picker.png', fullPage: true });

  // Select a year, e.g. 2022
  await page.getByRole('button', { name: '2022', exact: true }).click();
  await expect(page.locator('input[name="manufactureYear"]')).toHaveValue('2022');

  // Also open country code dropdown to capture the list with flags and calling codes
  await countryTrigger.click();
  await expect(page.getByRole('listbox')).toBeVisible();
  await page.screenshot({ path: 'scratch/country-flags-dropdown.png', fullPage: true });
});
