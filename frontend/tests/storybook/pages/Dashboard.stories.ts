import { Page } from 'puppeteer';
import Dashboard from '@/views/Dashboard';
import {
  CategoryDto,
  CustomerDto,
  IncomesExpensesStatisticsDto,
  IncomeTaxPaymentsStatisticsDto,
  InvoiceDto,
} from '@/services/api';
import { apiPage, onGet } from '../utils/stories-api-mocks';
import { Categories, Customers } from '../utils/stories-common-data';
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
          categoryId: Categories.category1.id,
          totalAmount: 1920223,
          finalizedCount: 3,
          pendingCount: 2,
        }, {
          categoryId: Categories.category2.id,
          totalAmount: 78392,
          finalizedCount: 1,
          pendingCount: 0,
        }],
        currencyExchangeDifference: 0,
        totalAmount: 1998615,
      } as IncomesExpensesStatisticsDto);

    onGet('/api/workspaces/42/statistics/incomes')
      .successJson({
        items: [{
          categoryId: Categories.category3.id,
          totalAmount: 7394307,
          finalizedCount: 12,
          pendingCount: 0,
        }, {
          categoryId: Categories.category4.id,
          totalAmount: 92893,
          finalizedCount: 341,
          pendingCount: 4,
        }],
        currencyExchangeDifference: 3782,
        totalAmount: 7487200,
      } as IncomesExpensesStatisticsDto);

    onGet('/api/workspaces/42/statistics/income-tax-payments')
      .successJson({
        totalTaxPayments: 478292,
      } as IncomeTaxPaymentsStatisticsDto);

    onGet('/api/workspaces/42/invoices')
      .successJson(apiPage<InvoiceDto>([{
        currency: 'USD',
        amount: 234289,
        customer: Customers.customer1.id,
        title: 'Invoice #706',
        status: 'SENT',
        dateIssued: '2030-01-02',
        dateSent: '2030-01-05',
        dueDate: '2030-06-23',
        timeRecorded: '',
        id: 1,
        version: 1,
        attachments: [],
        datePaid: null,
        notes: null,
        generalTax: null,
      }, {
        currency: 'EUR',
        amount: 98372,
        customer: Customers.customer1.id,
        title: 'Invoice #032',
        status: 'OVERDUE',
        dateIssued: '2030-01-02',
        dateSent: '2030-01-05',
        dueDate: '2030-02-01',
        timeRecorded: '',
        id: 2,
        version: 1,
        attachments: [],
        datePaid: null,
        notes: null,
        generalTax: null,
      }]));

    onGet('api/workspaces/42/categories')
      .successJson(apiPage<CategoryDto>([
        Categories.category1, Categories.category2, Categories.category3, Categories.category4,
      ]));

    onGet('api/workspaces/42/customers')
      .successJson(apiPage<CustomerDto>([Customers.customer1]));
  },
});
Loaded.parameters = {
  storyshots: {
    async setup(page: Page) {
      await setViewportHeight(page, 800);
    },
  },
};
