// noinspection JSUnusedGlobalSymbols

import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, openOverviewPanelDetails, waitForText,
} from '@/__storybook__/screenshots';
import IncomesOverviewPanel from '@/pages/incomes/IncomesOverviewPanel.vue';
import type { IncomeDto } from '@/services/api';
import { mockSuccessStorageStatus } from '@/__storybook__/api-mocks';
import { storybookData } from '@/__storybook__/storybook-data';

function mockApi() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
}

const incomePrototype: IncomeDto = {
  title: 'Delivery X',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  currency: 'USD',
  originalAmount: 4276,
  attachments: [],
  id: 42,
  version: 0,
  status: 'PENDING_CONVERSION',
  convertedAmounts: {},
  incomeTaxableAmounts: {},
  useDifferentExchangeRateForIncomeTaxPurposes: false,
  dateReceived: new Date('2020-01-09T00:00:00'),
};

function createStory(income: IncomeDto) {
  return {
    components: { IncomesOverviewPanel },
    data() {
      return { income };
    },
    template: '<IncomesOverviewPanel :income="income" />',
    beforeCreate() {
      mockApi();
    },
  };
}

export default {
  title: 'Pages/Incomes/IncomesOverviewPanel',
  parameters: {
    asPage: true,
    useRealTime: true,
    screenshotPreparation: allOf(
      openOverviewPanelDetails(),
      waitForText('Conversion to AUD pending'),
    ),
  },
};

export const Pending = defineStory(() => ({
  ...createStory({
    ...incomePrototype,
    status: 'PENDING_CONVERSION',
  }),
}));
