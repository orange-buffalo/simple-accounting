// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf,
  openOverviewPanelDetailsAndDisableAnimations,
  waitForOutputLoaderText,
} from '@/__storybook__/screenshots';
import InvoicesOverviewPanel from '@/pages/invoices/InvoicesOverviewPanel.vue';
import type { CustomerDto, InvoiceDto } from '@/services/api';
import { defaultWorkspacePath, fetchMock, pageResponse } from '@/__storybook__/api-mocks';

const customer: CustomerDto = {
  id: 77,
  name: 'Favourite Customer Ltd',
  version: 0,
};

function mockTaxesAndCustomers() {
  fetchMock.get(defaultWorkspacePath(`/customers/${customer.id}`), customer);
  fetchMock.get(defaultWorkspacePath('/general-taxes'), pageResponse());
}

const invoicePrototype: InvoiceDto = {
  title: 'Invoice #22041',
  customer: customer.id,
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
      mockTaxesAndCustomers();
    },
  };
}

export default {
  title: 'Pages/Invoices/InvoicesOverviewPanel',
  parameters: {
    asPage: true,
    screenshotPreparation: allOf(
      openOverviewPanelDetailsAndDisableAnimations(),
      waitForOutputLoaderText(customer.name),
    ),
  },
};

export const Draft = defineStory(() => ({
  ...createStory({
    ...invoicePrototype,
    status: 'DRAFT',
  }),
  beforeCreate() {
    mockTaxesAndCustomers();
    fetchMock.put(`path:${defaultWorkspacePath(`/invoices/${invoicePrototype.id}`)}`, (_, req) => {
      action('PUT Invoice API')(req.body);
      return {};
    });
  },
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
