import { action } from '@storybook/addon-actions';
import EditInvoice from '@/views/invoices/EditInvoice';
import { setViewportHeight } from '../../utils/stories-utils';
import {
  apiPage, onGet, onGetToWorkspacePath, onPostToWorkspacePath, onPutToWorkspacePath,
} from '../../utils/stories-api-mocks';

const customer = {
  id: 50,
  name: 'Customer',
};

const generalTax = {
  id: 100,
  title: 'Tax 1',
};

const invoice = {
  customer: 50,
  title: 'Invoice #42',
  currency: 'AUD',
  amount: 597890,
  attachments: [],
  id: 42,
  version: 0,
  dateIssued: '2020-04-05',
  dueDate: '2025-01-05',
  status: 'DRAFT',
};

function mockApiResources() {
  onGetToWorkspacePath('/customers')
    .successJson(apiPage([customer]));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([generalTax]));
  onGetToWorkspacePath('/statistics/currencies-shortlist')
    .successJson(['AUD', 'USD']);
  onGet('api/profile/documents-storage')
    .successJson({ active: true });
  onPutToWorkspacePath('/invoices/42')
    .intercept((req, res) => {
      action('PUT')(req.pathname, req.body);
      res.json(req.body);
    });
  onPostToWorkspacePath('/invoices')
    .intercept((req, res) => {
      action('POST')(req.pathname, req.body);
      res.json(req.body);
    });
  onPostToWorkspacePath('/invoices/42/cancel')
    .intercept((req, res) => {
      action('POST')(req.pathname, req.body);
      res.json({
        ...invoice,
        status: 'CANCELLED',
      });
    });
}

export default {
  title: 'Pages|Invoices/EditInvoice',
  parameters: {
    fullWidth: true,
    storyshots: {
      async setup(page) {
        await setViewportHeight(page, 1200);
      },
    },
  },
};

// noinspection JSUnusedGlobalSymbols
export const CreateNewInvoice = () => ({
  components: { EditInvoice },
  template: '<EditInvoice />',
  beforeCreate() {
    mockApiResources();
  },
});

export const EditExistingInvoice = () => ({
  components: { EditInvoice },
  template: '<EditInvoice :id="42" />',
  beforeCreate() {
    mockApiResources();
    onGetToWorkspacePath('/invoices/42')
      .successJson(invoice);
  },
});

export const EditCancelledInvoice = () => ({
  components: { EditInvoice },
  template: '<EditInvoice :id="42" />',
  beforeCreate() {
    mockApiResources();
    onGetToWorkspacePath('/invoices/42')
      .successJson({
        ...invoice,
        status: 'CANCELLED',
      });
  },
});
