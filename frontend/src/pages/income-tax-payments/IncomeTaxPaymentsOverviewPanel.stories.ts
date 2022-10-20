// noinspection JSUnusedGlobalSymbols

import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, openOverviewPanelDetails, waitForText,
} from '@/__storybook__/screenshots';
import IncomeTaxPaymentsOverviewPanel from '@/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue';
import type { IncomeTaxPaymentDto } from '@/services/api';
import { mockSuccessStorageStatus } from '@/__storybook__/api-mocks';
import { storybookData } from '@/__storybook__/storybook-data';

function mockApi() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
}

const taxPaymentPrototype: IncomeTaxPaymentDto = {
  title: 'Tax X',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  attachments: [],
  id: 42,
  version: 0,
  datePaid: new Date('2020-10-08'),
  reportingDate: new Date('2020-01-04'),
  amount: 434353,
};

function createStory(taxPayment: IncomeTaxPaymentDto) {
  return {
    components: { IncomeTaxPaymentsOverviewPanel },
    data() {
      return { taxPayment };
    },
    template: '<IncomeTaxPaymentsOverviewPanel :tax-payment="taxPayment" />',
    beforeCreate() {
      mockApi();
    },
  };
}

export default {
  title: 'Pages/IncomeTaxPayments/IncomeTaxPaymentsOverviewPanel',
  parameters: {
    asPage: true,
    useRealTime: true,
    screenshotPreparation: allOf(
      openOverviewPanelDetails(),
      waitForText('Reporting Date'),
    ),
  },
};

export const Default = defineStory(() => ({
  ...createStory({
    ...taxPaymentPrototype,
  }),
}));
