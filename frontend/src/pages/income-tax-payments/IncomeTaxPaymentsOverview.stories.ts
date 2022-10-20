// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { waitForText } from '@/__storybook__/screenshots';
import IncomeTaxPaymentsOverview from '@/pages/income-tax-payments/IncomeTaxPaymentsOverview.vue';
import type { IncomeTaxPaymentDto } from '@/services/api';
import { onGetToDefaultWorkspacePath, pageResponse } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Pages/IncomeTaxPayments/IncomeTaxPaymentsOverview',
};

function mockApiResponses() {
  onGetToDefaultWorkspacePath('/income-tax-payments', (url) => {
    action('search')(url);
    return pageResponse<IncomeTaxPaymentDto>({
      title: 'Tax X',
      timeRecorded: new Date('2020-01-04T00:00:00'),
      attachments: [],
      id: 42,
      version: 0,
      datePaid: new Date('2020-10-08'),
      reportingDate: new Date('2020-01-04'),
      amount: 434353,
    }, {
      title: 'Tax Y',
      timeRecorded: new Date('2020-01-04T00:00:00'),
      attachments: [],
      id: 43,
      version: 0,
      datePaid: new Date('2021-10-08'),
      reportingDate: new Date('2021-01-04'),
      amount: 34232,
    });
  });
}

export const Default = defineStory(() => ({
  components: { IncomeTaxPaymentsOverview },
  template: '<IncomeTaxPaymentsOverview />',
  beforeCreate() {
    mockApiResponses();
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: waitForText('Tax X'),
});

export const ReadOnly = defineStory(() => ({
  components: { IncomeTaxPaymentsOverview },
  template: '<IncomeTaxPaymentsOverview />',
  beforeCreate() {
    mockApiResponses();
  },
}), {
  workspace: {
    editable: false,
  },
  asPage: true,
  useRealTime: true,
  screenshotPreparation: waitForText('Tax X'),
});
