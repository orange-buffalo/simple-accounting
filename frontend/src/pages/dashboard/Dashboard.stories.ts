// noinspection JSUnusedGlobalSymbols

import Dashboard from '@/pages/dashboard/Dashboard.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, disableIconsSvgAnimations, waitForText } from '@/__storybook__/screenshots';
import {
  neverEndingGetRequest, onGetToDefaultWorkspacePath, pageResponse,
} from '@/__storybook__/api-mocks';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Dashboard',
};

export const Loading = defineStory(() => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/statistics/expenses', {}, neverEndingGetRequest);
    onGetToDefaultWorkspacePath('/statistics/incomes', {}, neverEndingGetRequest);
    onGetToDefaultWorkspacePath('/statistics/income-tax-payments', {}, neverEndingGetRequest);
    onGetToDefaultWorkspacePath('/invoices', {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableIconsSvgAnimations(),
});

export const Loaded = defineStory(() => ({
  components: { Dashboard },
  template: '<Dashboard/>',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/statistics/expenses', {
      totalAmount: 26182,
      finalizedCount: 42,
      pendingCount: 10,
      currencyExchangeDifference: 0,
      items: [{
        categoryId: storybookData.categories.slurmCategory.id,
        totalAmount: 2232,
      },
        {
          totalAmount: 849,
        },
      ],
    });
    onGetToDefaultWorkspacePath('/statistics/incomes', {
      totalAmount: 388838,
      finalizedCount: 19,
      pendingCount: 2,
      currencyExchangeDifference: 82422,
      items: [{
        categoryId: storybookData.categories.planetExpressCategory.id,
        totalAmount: 382929,
      },
      ],
    });
    onGetToDefaultWorkspacePath('/statistics/income-tax-payments', {
      totalTaxPayments: 63839,
    });
    onGetToDefaultWorkspacePath('/invoices', pageResponse({
      title: 'Overdue Invoice #1234',
      customer: storybookData.customers.governmentOfEarth.id,
      dateIssued: new Date('2021-02-12'),
      dateSent: new Date('2021-02-15'),
      dueDate: new Date('2021-02-15'),
      status: 'OVERDUE',
      currency: 'EUR',
      amount: 3923,
    }, {
      title: 'Pending Invoice #09876',
      customer: storybookData.customers.democraticOrderOfPlanets.id,
      dateIssued: new Date('2021-04-01'),
      dateSent: new Date('2021-04-02'),
      dueDate: new Date('2021-05-30'),
      status: 'PENDING',
      currency: 'USD',
      amount: 37242,
    }));
    storybookData.mockApi();
  },
}), {
  screenshotPreparation: allOf(
    waitForText(storybookData.categories.planetExpressCategory.name),
    waitForText(storybookData.categories.slurmCategory.name),
    waitForText('Profit'),
    waitForText(storybookData.customers.democraticOrderOfPlanets.name),
    waitForText(storybookData.customers.governmentOfEarth.name),
  ),
});
