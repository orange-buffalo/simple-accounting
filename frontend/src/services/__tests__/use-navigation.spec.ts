import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { EXPENSES_OVERVIEW_ROUTE_NAME } from '@/pages/expenses/ExpensesOverview.vue';
import { EDIT_EXPENSE_ROUTE_NAME } from '@/pages/expenses/EditExpense.vue';
import { INVOICES_OVERVIEW_ROUTE_NAME } from '@/pages/invoices/InvoicesOverview.vue';
import useNavigation from '@/services/use-navigation';

const { useRouterMock } = vi.hoisted(() => ({ useRouterMock: vi.fn() }));

vi.mock('vue-router', async (importOriginal) => ({
  ...await importOriginal<typeof import('vue-router')>(),
  useRouter: useRouterMock,
}));

describe('navigation', () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: {} },
      { path: '/expenses', name: 'expenses-overview', component: {} },
      { path: '/expenses/:id/edit', name: 'edit-expense', component: {} },
      { path: '/invoices', name: 'invoices-overview', component: {} },
    ],
  });

  beforeEach(async () => {
    useRouterMock.mockReturnValue(router);
    await router.push('/');
  });

  it('navigates by a shared route name', async () => {
    await useNavigation().navigateByViewName(EXPENSES_OVERVIEW_ROUTE_NAME);

    expect(router.currentRoute.value.path).toBe('/expenses');
  });

  it('preserves route parameters', async () => {
    await useNavigation().navigateToView({ name: EDIT_EXPENSE_ROUTE_NAME, params: { id: '42' } });

    expect(router.currentRoute.value.path).toBe('/expenses/42/edit');
  });

  it('preserves search queries', async () => {
    await useNavigation().navigateToView({
      name: INVOICES_OVERVIEW_ROUTE_NAME,
      query: { freeSearchText: 'Slurm & supplies' },
    });

    expect(router.currentRoute.value.path).toBe('/invoices');
    expect(router.currentRoute.value.query).toEqual({ freeSearchText: 'Slurm & supplies' });
  });
});
