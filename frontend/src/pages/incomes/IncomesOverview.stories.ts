// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { waitForText } from '@/__storybook__/screenshots';
import IncomesOverview from '@/pages/incomes/IncomesOverview.vue';
import type { IncomeDto } from '@/services/api';
import { onGetToDefaultWorkspacePath, pageResponse } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Incomes/IncomesOverview',
};

function mockApiResponses() {
  storybookData.mockApi();
  onGetToDefaultWorkspacePath('/incomes', (url) => {
    action('income-search')(url);
    return pageResponse<IncomeDto>({
      title: 'Delivery X',
      timeRecorded: new Date('2020-01-04T00:00:00'),
      currency: 'USD',
      originalAmount: 4276,
      attachments: [],
      id: 42,
      version: 0,
      status: 'PENDING_CONVERSION',
      dateReceived: new Date('2020-02-10'),
      convertedAmounts: {},
      incomeTaxableAmounts: {},
      useDifferentExchangeRateForIncomeTaxPurposes: false,
    }, {
      title: 'Deliver Y',
      timeRecorded: new Date('2020-03-04T00:00:00'),
      currency: 'EUR',
      originalAmount: 450,
      attachments: [],
      id: 43,
      version: 0,
      status: 'FINALIZED',
      dateReceived: new Date('2020-01-10'),
      category: storybookData.categories.planetExpressCategory.id,
      generalTax: storybookData.generalTaxes.planetExpressTax.id,
      generalTaxAmount: 20,
      generalTaxRateInBps: 50,
      notes: 'Best coffee _ever_',
      convertedAmounts: {
        adjustedAmountInDefaultCurrency: 550,
        originalAmountInDefaultCurrency: 450,
      },
      incomeTaxableAmounts: {
        originalAmountInDefaultCurrency: 225,
        adjustedAmountInDefaultCurrency: 340,
      },
      useDifferentExchangeRateForIncomeTaxPurposes: true,
    });
  });
}

export const Default = defineStory(() => ({
  components: { IncomesOverview },
  template: '<IncomesOverview />',
  setup() {
    mockApiResponses();
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: waitForText('Delivery X'),
});

export const ReadOnly = defineStory(() => ({
  components: { IncomesOverview },
  template: '<IncomesOverview />',
  setup() {
    mockApiResponses();
  },
}), {
  workspace: {
    editable: false,
  },
  asPage: true,
  useRealTime: true,
  screenshotPreparation: waitForText('Delivery X'),
});
