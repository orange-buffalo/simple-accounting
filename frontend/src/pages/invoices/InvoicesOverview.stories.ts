// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { allOf, waitForText } from '@/__storybook__/screenshots';
import InvoicesOverview from '@/pages/invoices/InvoicesOverview.vue';
import type { InvoiceDto } from '@/services/api';
import { onGetToDefaultWorkspacePath, pageResponse } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Invoices/InvoicesOverview',
};

function mockApiResponses() {
  storybookData.mockApi();
  onGetToDefaultWorkspacePath('/invoices', (url) => {
    action('invoice-search')(url);
    return pageResponse<InvoiceDto>({
      title: 'Invoice #22041',
      customer: storybookData.customers.governmentOfEarth.id,
      timeRecorded: new Date('2020-01-04T00:00:00'),
      dateIssued: new Date('2020-05-03'),
      dueDate: new Date('2030-01-01'),
      currency: 'AUD',
      amount: 4276,
      status: 'PAID',
      attachments: [],
      id: 42,
      version: 0,
    }, {
      title: 'Invoice #89220',
      customer: storybookData.customers.democraticOrderOfPlanets.id,
      generalTax: storybookData.generalTaxes.planetExpressTax.id,
      timeRecorded: new Date('2020-06-02T00:00:00'),
      dateIssued: new Date('2020-09-20'),
      dueDate: new Date('2060-07-10'),
      currency: 'EUR',
      amount: 390324,
      status: 'DRAFT',
      attachments: [],
      id: 43,
      version: 0,
    });
  });
}

export const Default = defineStory(() => ({
  components: { InvoicesOverview },
  template: '<InvoicesOverview />',
  beforeCreate() {
    mockApiResponses();
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: allOf(
    waitForText(storybookData.customers.governmentOfEarth.name),
    waitForText(storybookData.customers.democraticOrderOfPlanets.name),
  ),
});

export const ReadOnly = defineStory(() => ({
  components: { InvoicesOverview },
  template: '<InvoicesOverview />',
  beforeCreate() {
    mockApiResponses();
  },
}), {
  workspace: {
    editable: false,
  },
  asPage: true,
  useRealTime: true,
  screenshotPreparation: allOf(
    waitForText(storybookData.customers.governmentOfEarth.name),
    waitForText(storybookData.customers.democraticOrderOfPlanets.name),
  ),
});
