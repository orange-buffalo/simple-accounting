import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { ROUTE_NAMES } from '@/services/route-names';
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
    await useNavigation().navigateByViewName(ROUTE_NAMES.expensesOverview);

    expect(router.currentRoute.value.path).toBe('/expenses');
  });

  it('preserves route parameters', async () => {
    await useNavigation().navigateToView({ name: ROUTE_NAMES.editExpense, params: { id: '42' } });

    expect(router.currentRoute.value.path).toBe('/expenses/42/edit');
  });

  it('preserves search queries', async () => {
    await useNavigation().navigateToView({
      name: ROUTE_NAMES.invoicesOverview,
      query: { freeSearchText: 'Slurm & supplies' },
    });

    expect(router.currentRoute.value.path).toBe('/invoices');
    expect(router.currentRoute.value.query).toEqual({ freeSearchText: 'Slurm & supplies' });
  });
});
