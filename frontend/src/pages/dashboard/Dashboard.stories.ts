// noinspection JSUnusedGlobalSymbols

import Dashboard from '@/pages/dashboard/Dashboard.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, disableIconsSvgAnimations, waitForText } from '@/__storybook__/screenshots';
import {
  defaultWorkspacePath, fetchMock, neverEndingGetRequest, pageResponse,
} from '@/__storybook__/api-mocks';

export default {
  title: 'Pages/Dashboard',
};

export const Loading = defineStory(() => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/expenses')}`, {}, neverEndingGetRequest);
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/incomes')}`, {}, neverEndingGetRequest);
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/income-tax-payments')}`, {}, neverEndingGetRequest);
    fetchMock.get(`path:${defaultWorkspacePath('/invoices')}`, {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableIconsSvgAnimations(),
});

export const Loaded = defineStory(() => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/expenses')}`, {
      totalAmount: 26182,
      finalizedCount: 42,
      pendingCount: 10,
      currencyExchangeDifference: 0,
      items: [{
        categoryId: 1,
        totalAmount: 2232,
      },
        {
          totalAmount: 849,
        },
      ],
    });
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/incomes')}`, {
      totalAmount: 388838,
      finalizedCount: 19,
      pendingCount: 2,
      currencyExchangeDifference: 82422,
      items: [{
        categoryId: 2,
        totalAmount: 382929,
      },
      ],
    });
    fetchMock.get(`path:${defaultWorkspacePath('/statistics/income-tax-payments')}`, {
      totalTaxPayments: 63839,
    });
    fetchMock.get(defaultWorkspacePath('/categories'), {
      body: {
        data: [{
          id: 1,
          name: 'Category 1',
        }, {
          id: 2,
          name: 'Category 2',
        }],
      },
    });
    fetchMock.get(`path:${defaultWorkspacePath('/invoices')}`, pageResponse({
      title: 'Overdue Invoice #1234',
      customer: 7,
      dateIssued: new Date('2021-02-12'),
      dateSent: new Date('2021-02-15'),
      dueDate: new Date('2021-02-15'),
      status: 'OVERDUE',
      currency: 'EUR',
      amount: 3923,
    }, {
      title: 'Pending Invoice #09876',
      customer: 8,
      dateIssued: new Date('2021-04-01'),
      dateSent: new Date('2021-04-02'),
      dueDate: new Date('2021-05-30'),
      status: 'PENDING',
      currency: 'USD',
      amount: 37242,
    }));
    fetchMock.get(defaultWorkspacePath('/customers/7'), {
      id: 7,
      name: 'Customer A',
    });
    fetchMock.get(defaultWorkspacePath('/customers/8'), {
      id: 8,
      name: 'Customer B',
    });
  },
}), {
  screenshotPreparation: allOf(
    waitForText('Category 1'),
    waitForText('Category 2'),
    waitForText('Profit'),
    waitForText('Customer A'),
    waitForText('Customer B'),
  ),
});
