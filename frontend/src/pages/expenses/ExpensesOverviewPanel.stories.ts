// noinspection JSUnusedGlobalSymbols

import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, openOverviewPanelDetails, waitForText,
} from '@/__storybook__/screenshots';
import ExpensesOverviewPanel from '@/pages/expenses/ExpensesOverviewPanel.vue';
import type { ExpenseDto } from '@/services/api';
import {
  mockSuccessStorageStatus,
  onGetToDefaultWorkspacePath,
  pageResponse,
} from '@/__storybook__/api-mocks';
import { storybookData } from '@/__storybook__/storybook-data';

function mockApi() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
  onGetToDefaultWorkspacePath(
    '/documents',
    pageResponse(
      storybookData.documents.cheesePizzaAndALargeSodaReceipt,
      storybookData.documents.lunaParkDeliveryAgreement,
    ),
  );
}

const expensePrototype: ExpenseDto = {
  title: 'Pizza',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  currency: 'USD',
  originalAmount: 4276,
  attachments: [],
  id: 42,
  version: 0,
  status: 'PENDING_CONVERSION',
  datePaid: new Date('2020-02-10'),
  percentOnBusiness: 100,
  convertedAmounts: {},
  incomeTaxableAmounts: {},
  useDifferentExchangeRateForIncomeTaxPurposes: false,
};

function createStory(expense: ExpenseDto) {
  return {
    components: { ExpensesOverviewPanel },
    data() {
      return { expense };
    },
    template: '<ExpensesOverviewPanel :expense="expense" />',
    setup() {
      mockApi();
    },
  };
}

export default {
  title: 'Pages/Expenses/ExpensesOverviewPanel',
  parameters: {
    asPage: true,
    useRealTime: true,
    screenshotPreparation: allOf(
      openOverviewPanelDetails(),
      waitForText('Not specified'),
    ),
  },
};

export const Pending = defineStory(() => ({
  ...createStory({
    ...expensePrototype,
    status: 'PENDING_CONVERSION',
  }),
}));
