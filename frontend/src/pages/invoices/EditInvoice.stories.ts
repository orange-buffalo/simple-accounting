// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import {
  allOf, waitForInputLoadersToLoad, waitForText,
} from '@/__storybook__/screenshots';
import EditInvoice from '@/pages/invoices/EditInvoice.vue';
import type { InvoiceDto } from '@/services/api';
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
  title: 'Pages/Invoices/EditInvoice',
};

function mockApiResponses() {
  storybookData.mockApi();
  mockSuccessStorageStatus();
  mockDefaultWorkspaceCurrenciesShortlist();
}

export const Create = defineStory(() => ({
  components: { EditInvoice },
  template: '<EditInvoice />',
  beforeCreate() {
    mockApiResponses();
    fetchMock.post(defaultWorkspacePath('/invoices'), (_, req) => {
      action('POST /invoices')(req.body);
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

const invoiceProto = {
  title: 'Invoice #22041',
  customer: storybookData.customers.governmentOfEarth.id,
  timeRecorded: new Date('2020-01-04T00:00:00'),
  dateIssued: new Date('2020-05-03'),
  dueDate: new Date('2030-01-01'),
  dateSent: new Date('2030-01-05'),
  datePaid: new Date('2030-01-10'),
  currency: 'AUD',
  amount: 4276,
  status: 'PAID',
  attachments: [],
  id: 987,
  version: 0,
  notes: '_Notes formatted_',
} as InvoiceDto;

export const Edit = defineStory(() => ({
  components: { EditInvoice },
  template: '<EditInvoice :id="987" />',
  beforeCreate() {
    mockApiResponses();
    onGetToDefaultWorkspacePath('/invoices/987', invoiceProto);
    fetchMock.put(defaultWorkspacePath('/invoices/987'), (_, req) => {
      action('PUT /invoices')(JSON.parse(req.body as string));
      return {};
    });
    fetchMock.post(defaultWorkspacePath('/invoices/987/cancel'), () => {
      action('POST /invoices/cancel')();
      return {
        ...invoiceProto,
        status: 'CANCELLED',
      } as InvoiceDto;
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
