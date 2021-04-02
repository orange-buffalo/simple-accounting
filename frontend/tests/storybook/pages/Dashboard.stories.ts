import { Page } from 'puppeteer';
import Dashboard from '@/views/Dashboard';
import { Components } from '@/services/api/api-client-definition';
import { onGet } from '../utils/stories-api-mocks';
import { pauseAndResetAnimation, setViewportHeight } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Dashboard',
  parameters: {
    fullWidth: true,
  },
};

export const Loading = () => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    onGet('/api/workspaces/42/statistics/expenses')
      .neverEndingRequest();
    onGet('/api/workspaces/42/statistics/incomes')
      .neverEndingRequest();
    onGet('/api/workspaces/42/statistics/income-tax-payments')
      .neverEndingRequest();
    onGet('/api/workspaces/42/invoices')
      .neverEndingRequest();
  },
});
Loading.parameters = {
  storyshots: {
    async setup(page: Page) {
      await setViewportHeight(page, 350);
      await pauseAndResetAnimation(page, '.el-icon-loading');
    },
  },
};

export const Loaded = () => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    onGet('/api/workspaces/42/statistics/expenses')
      .successJson({
        items: [{
          categoryId: 1,
          totalAmount: 1920223,
          finalizedCount: 3,
          pendingCount: 2,
        }, {
          categoryId: 2,
          totalAmount: 78392,
          finalizedCount: 1,
          pendingCount: 0,
        }],
        currencyExchangeDifference: 0,
        totalAmount: 1998615,
      } as Components.Schemas.IncomesExpensesStatisticsDto);

    onGet('/api/workspaces/42/statistics/incomes')
      .successJson({
        items: [{
          categoryId: 3,
          totalAmount: 7394307,
          finalizedCount: 12,
          pendingCount: 0,
        }, {
          categoryId: 4,
          totalAmount: 92893,
          finalizedCount: 341,
          pendingCount: 4,
        }],
        currencyExchangeDifference: 3782,
        totalAmount: 7487200,
      } as Components.Schemas.IncomesExpensesStatisticsDto);

    onGet('/api/workspaces/42/statistics/income-tax-payments')
      .successJson({
        totalTaxPayments: 478292,
      } as Components.Schemas.IncomeTaxPaymentsStatisticsDto);

    onGet('/api/workspaces/42/invoices')
      .successJson({
        totalElements: 0,
        pageSize: 0,
        data: [{
          currency: 'USD',
          amount: 234289,
          customer: 1,
          title: 'Invoice #706',
          status: 'SENT',
          dateIssued: '2030-01-02',
          dateSent: '2030-01-05',
          dueDate: '2030-06-23',
        }, {
          currency: 'EUR',
          amount: 98372,
          customer: 1,
          title: 'Invoice #032',
          status: 'OVERDUE',
          dateIssued: '2030-01-02',
          dateSent: '2030-01-05',
          dueDate: '2030-02-01',
        }],
      } as Components.Schemas.ApiPageInvoiceDto);

    onGet('api/workspaces/42/categories')
      .successJson({
        totalElements: 4,
        pageSize: 4,
        data: [{
          id: 1,
          name: 'Category 1',
        }, {
          id: 2,
          name: 'Category 2',
        }, {
          id: 3,
          name: 'Category 3',
        }, {
          id: 4,
          name: 'Category 4',
        }],
      } as Components.Schemas.ApiPageCategoryDto);

    onGet('api/workspaces/42/customers')
      .successJson({
        totalElements: 1,
        pageSize: 1,
        data: [{
          id: 1,
          name: 'Customer 1',
        }],
      } as Components.Schemas.ApiPageCustomerDto);
  },
});
Loaded.parameters = {
  storyshots: {
    async setup(page: Page) {
      await setViewportHeight(page, 800);
    },
  },
};
