import { expect, test } from '@playwright/test';

const mockLogin = async (
  page,
  {
    email,
    displayName,
    roles = [],
    managedVenueIds = [],
  }: {
    email: string;
    displayName: string;
    roles?: string[];
    managedVenueIds?: string[];
  },
) => {
  await page.goto('/login');

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Display Name').fill(displayName);

  for (const role of roles) {
    if (role === 'ROLE_MANAGER') {
      await page.getByLabel('Sign in with manager role').check();
      if (managedVenueIds.length > 0) {
        await page
          .getByLabel('Managed Venue IDs (comma separated UUIDs)')
          .fill(managedVenueIds.join(','));
      }
    }
    if (role === 'ROLE_ADMIN') {
      await page.getByLabel('Sign in with admin role').check();
    }
  }

  await Promise.all([
    page.waitForLoadState('networkidle'),
    page.getByRole('button', { name: /start session/i }).click(),
  ]);
};

test.describe('Primary Ticket Outlet SPA', () => {
  test('user login shows attendee view', async ({ page }) => {
    await mockLogin(page, {
      email: 'user.e2e@example.com',
      displayName: 'E2E User',
      roles: ['ROLE_USER'],
    });

    await expect(
      page.getByRole('button', { name: 'Attendee', exact: true }),
    ).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByText(/upcoming events/i)).toBeVisible();
  });

  test('manager can switch role and see venue controls', async ({ page }) => {
    await mockLogin(page, {
      email: 'manager.e2e@example.com',
      displayName: 'Venue Manager',
      roles: ['ROLE_USER', 'ROLE_MANAGER'],
      managedVenueIds: ['11111111-1111-1111-1111-111111111111'],
    });

    await page.getByRole('button', { name: 'Manager' }).click();
    await expect(page.getByRole('heading', { name: /create event/i })).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Generate Tickets' }).first()
    ).toBeVisible();
  });

  test('admin dashboard lists venues and events', async ({ page }) => {
    await mockLogin(page, {
      email: 'admin.e2e@example.com',
      displayName: 'Admin User',
      roles: ['ROLE_USER', 'ROLE_ADMIN'],
    });

    await page.getByRole('button', { name: 'Admin' }).click();
    await expect(page.getByRole('heading', { name: /venues/i })).toBeVisible();
    await expect(page.getByText(/tickets sold/i).first()).toBeVisible();
  });
});
