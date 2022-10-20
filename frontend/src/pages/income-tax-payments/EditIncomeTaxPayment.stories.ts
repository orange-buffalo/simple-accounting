// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { allOf, waitForText } from '@/__storybook__/screenshots';
import EditIncomeTaxPayment from '@/pages/income-tax-payments/EditIncomeTaxPayment.vue';
import type { IncomeTaxPaymentDto } from '@/services/api';
import {
  defaultWorkspacePath,
  fetchMock,
  mockSuccessStorageStatus,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Pages/IncomeTaxPayments/EditIncomeTaxPayment',
};

function mockApiResponses() {
  mockSuccessStorageStatus();
}

export const Create = defineStory(() => ({
  components: { EditIncomeTaxPayment },
  template: '<EditIncomeTaxPayment />',
  beforeCreate() {
    mockApiResponses();
    fetchMock.post(defaultWorkspacePath('/income-tax-payments'), (_, req) => {
      action('POST /income-tax-payments')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  screenshotPreparation: allOf(
    waitForText('Drop file here or click to upload'),
  ),
});

const incomeProto = {
  title: 'Tax X',
  timeRecorded: new Date('2020-01-04T00:00:00'),
  attachments: [],
  id: 987,
  version: 0,
  datePaid: new Date('2020-10-08'),
  reportingDate: new Date('2020-01-04'),
  amount: 434353,
  notes: '_Notes formatted_',
} as IncomeTaxPaymentDto;

export const Edit = defineStory(() => ({
  components: { EditIncomeTaxPayment },
  template: '<EditIncomeTaxPayment :id="987" />',
  beforeCreate() {
    mockApiResponses();
    onGetToDefaultWorkspacePath('/income-tax-payments/987', incomeProto);
    fetchMock.put(defaultWorkspacePath('/income-tax-payments/987'), (_, req) => {
      action('PUT /income-tax-payments')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: allOf(
    waitForText('Drop file here or click to upload'),
    waitForText('Notes formatted', 'em'),
  ),
});
