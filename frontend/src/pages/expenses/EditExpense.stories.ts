// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import {
  allOf, waitForInputLoadersToLoad, waitForText,
} from '@/__storybook__/screenshots';
import EditExpense from '@/pages/expenses/EditExpense.vue';
import type { ExpenseDto } from '@/services/api';
import {
  defaultWorkspacePath,
  fetchMock,
  mockDefaultWorkspaceCurrenciesShortlist,
  mockSuccessStorageStatus,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Expenses/EditExpense',
};

function mockApiResponses() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
  mockDefaultWorkspaceCurrenciesShortlist();
}

export const Create = defineStory(() => ({
  components: { EditExpense },
  template: '<EditExpense />',
  beforeCreate() {
    mockApiResponses();
    fetchMock.post(defaultWorkspacePath('/expenses'), (_, req) => {
      action('POST /expenses')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  screenshotPreparation: allOf(
    waitForText('Drop file here or click to upload'),
    waitForInputLoadersToLoad(),
  ),
});

const expenseProto = {
  title: 'Coffee',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  datePaid: new Date('2030-01-10'),
  currency: 'AUD',
  generalTax: storybookData.generalTaxes.planetExpressTax.id,
  category: storybookData.categories.planetExpressCategory.id,
  originalAmount: 4276,
  status: 'FINALIZED',
  useDifferentExchangeRateForIncomeTaxPurposes: false,
  percentOnBusiness: 100,
  incomeTaxableAmounts: {
    adjustedAmountInDefaultCurrency: 4276,
    originalAmountInDefaultCurrency: 4276,
  },
  convertedAmounts: {
    originalAmountInDefaultCurrency: 4276,
    adjustedAmountInDefaultCurrency: 4276,
  },
  attachments: [],
  id: 987,
  version: 0,
  notes: '_Notes formatted_',
} as ExpenseDto;

export const Edit = defineStory(() => ({
  components: { EditExpense },
  template: '<EditExpense :id="987" />',
  beforeCreate() {
    mockApiResponses();
    onGetToDefaultWorkspacePath('/expenses/987', expenseProto);
    fetchMock.put(defaultWorkspacePath('/expenses/987'), (_, req) => {
      action('PUT /expenses')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: allOf(
    waitForText('Drop file here or click to upload'),
    waitForText('Notes formatted'),
    waitForInputLoadersToLoad(),
  ),
});
