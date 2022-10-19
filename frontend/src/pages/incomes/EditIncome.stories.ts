// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import {
  allOf, waitForInputLoadersToLoad, waitForText,
} from '@/__storybook__/screenshots';
import EditIncome from '@/pages/incomes/EditIncome.vue';
import type { IncomeDto } from '@/services/api';
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
  title: 'Pages/Incomes/EditIncome',
};

function mockApiResponses() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
  mockDefaultWorkspaceCurrenciesShortlist();
}

export const Create = defineStory(() => ({
  components: { EditIncome },
  template: '<EditIncome />',
  beforeCreate() {
    mockApiResponses();
    fetchMock.post(defaultWorkspacePath('/incomes'), (_, req) => {
      action('POST /incomes')(JSON.parse(req.body as string));
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

const incomeProto = {
  title: 'Delivery',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  dateReceived: new Date('2020-01-04'),
  currency: 'AUD',
  generalTax: storybookData.generalTaxes.planetExpressTax.id,
  category: storybookData.categories.planetExpressCategory.id,
  originalAmount: 4276,
  status: 'FINALIZED',
  useDifferentExchangeRateForIncomeTaxPurposes: false,
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
} as IncomeDto;

export const Edit = defineStory(() => ({
  components: { EditIncome },
  template: '<EditIncome :id="987" />',
  beforeCreate() {
    mockApiResponses();
    onGetToDefaultWorkspacePath('/incomes/987', incomeProto);
    fetchMock.put(defaultWorkspacePath('/incomes/987'), (_, req) => {
      action('PUT /incomes')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: allOf(
    waitForText('Drop file here or click to upload'),
    waitForText('Notes formatted', 'em'),
    waitForInputLoadersToLoad(),
  ),
});
