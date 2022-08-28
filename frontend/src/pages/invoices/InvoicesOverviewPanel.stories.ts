// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, openOverviewPanelDetails, waitForText,
} from '@/__storybook__/screenshots';
import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
import type { InvoiceDto } from '@/services/api';
import {
  defaultWorkspacePath,
  fetchMock,
  onGetToDefaultWorkspacePath,
  pageResponse,
  pathOnlyMatcher,
} from '@/__storybook__/api-mocks';
import { storybookData } from '@/__storybook__/storybook-data';

function mockApi() {
  storybookData.mockApi();
  fetchMock.get('/api/profile/documents-storage', {
    active: true,
  });
  onGetToDefaultWorkspacePath(
    '/documents',
    pageResponse(
      storybookData.documents.cheesePizzaAndALargeSodaReceipt,
      storybookData.documents.lunaParkDeliveryAgreement,
    ),
  );
}

const invoicePrototype: InvoiceDto = {
  title: 'Invoice #22041',
  customer: storybookData.customers.governmentOfEarth.id,
  timeRecorded: new Date('2020-01-04T00:00:00'),
  dateIssued: new Date('2020-05-03'),
  dueDate: new Date('2030-01-01'),
  currency: 'AUD',
  amount: 4276,
  attachments: [],
  id: 42,
  version: 0,
  status: 'DRAFT',
};

function createStory(invoice: InvoiceDto) {
  return {
    components: { InvoicesOverviewPanel },
    data() {
      return { invoice };
    },
    methods: {
      onInvoiceUpdate() {
        action('invoice-update')();
      },
    },
    template: '<InvoicesOverviewPanel :invoice="invoice" @invoice-update="onInvoiceUpdate" />',
    beforeCreate() {
      mockApi();
      fetchMock.put(pathOnlyMatcher(defaultWorkspacePath(`/invoices/${invoicePrototype.id}`)), (_, req) => {
        action('PUT Invoice API')(req.body);
        return {};
      });
    },
  };
}

export default {
  title: 'Pages/Invoices/InvoicesOverviewPanel',
  parameters: {
    asPage: true,
    useRealTime: true,
    screenshotPreparation: allOf(
      openOverviewPanelDetails(),
      waitForText(storybookData.customers.governmentOfEarth.name),
    ),
  },
};

export const Draft = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    status: 'DRAFT',
  }),
}));

export const Sent = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    dateSent: new Date('2020-05-04'),
    status: 'SENT',
  }),
}));

export const Overdue = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    dateSent: new Date('2020-05-04'),
    status: 'OVERDUE',
  }),
}));

export const Paid = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    dateSent: new Date('2020-05-04'),
    datePaid: new Date('2020-06-15'),
    status: 'PAID',
  }),
}));

export const Cancelled = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    status: 'CANCELLED',
  }),
}));

export const ReadOnlyWorkspace = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    status: 'DRAFT',
  }),
}), {
  workspace: {
    editable: false,
  },
});

export const WithAllDetails = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    status: 'DRAFT',
    generalTax: storybookData.generalTaxes.planetExpressTax.id,
    attachments: [
      storybookData.documents.cheesePizzaAndALargeSodaReceipt.id,
      storybookData.documents.lunaParkDeliveryAgreement.id,
    ],
    notes: 'Some notes _with formatting_',
  }),
}), {
  screenshotPreparation: allOf(
    openOverviewPanelDetails(),
    waitForText(storybookData.customers.governmentOfEarth.name),
    waitForText(storybookData.generalTaxes.planetExpressTax.title),
    waitForText(storybookData.documents.cheesePizzaAndALargeSodaReceipt.name),
    waitForText('with formatting'),
  ),
});
