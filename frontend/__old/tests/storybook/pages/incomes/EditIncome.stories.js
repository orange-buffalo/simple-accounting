import { action } from '@storybook/addon-actions';
import EditIncome from '@/views/incomes/EditIncome';
import {
  apiPage,
  onGet,
  onGetToWorkspacePath,
  onPostToWorkspacePath,
  onPutToWorkspacePath,
} from '../../utils/stories-api-mocks';
import { setViewportHeight } from '../../utils/stories-utils';

const category = {
  id: 13,
  version: 0,
  name: 'Category 1',
  income: true,
  expense: true,
};

const generalTax = {
  id: 100,
  title: 'Tax 1',
};

const incomeProto = {
  category: null,
  title: 'Payment for Invoice #22',
  timeRecorded: '2020-01-04T00:00:00',
  dateReceived: '2020-05-03',
  currency: 'AUD',
  originalAmount: 6859,
  attachments: [],
  notes: null,
  id: 42,
  version: 0,
  status: 'FINALIZED',
  linkedInvoice: null,
  generalTax: null,
  generalTaxRateInBps: null,
  generalTaxAmount: null,
  convertedAmounts: {
    originalAmountInDefaultCurrency: 6859,
    adjustedAmountInDefaultCurrency: 6859,
  },
  incomeTaxableAmounts: {
    originalAmountInDefaultCurrency: 6859,
    adjustedAmountInDefaultCurrency: 6859,
  },
  useDifferentExchangeRateForIncomeTaxPurposes: false,
};

function generateInvoices() {
  const invoices = [];
  for (let i = 0; i < 25; i += 1) {
    const id = 42 + i;
    invoices.push({
      title: `Invoice #2204${i}-${id}`,
      dateIssued: '2020-05-03',
      currency: 'AUD',
      amount: 4276 + i * 139,
      id,
    });
  }
  return invoices;
}

function mockApiResources() {
  onGetToWorkspacePath('/categories')
    .successJson(apiPage([category]));
  onGetToWorkspacePath('/invoices')
    .successJson(apiPage(generateInvoices()));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([generalTax]));
  onGetToWorkspacePath('/statistics/currencies-shortlist')
    .successJson(['AUD', 'USD']);
  onGet('api/profile/documents-storage')
    .successJson({ active: true });
  onPutToWorkspacePath('/incomes/42')
    .intercept((req, res) => {
      action('PUT')(req.pathname, req.body);
      res.json(req.body);
    });
  onGetToWorkspacePath('/invoices/100')
    .successJson({
      title: 'Invoice #100',
      dateIssued: '2020-05-03',
      currency: 'AUD',
      amount: 4276,
      id: 100,
    });
  onPostToWorkspacePath('/incomes')
    .intercept((req, res) => {
      action('POST')(req.pathname, req.body);
      res.json(req.body);
    });
}

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Incomes/EditIncome',
  parameters: {
    fullWidth: true,
    storyshots: {
      async setup(page) {
        await setViewportHeight(page, 1100);
      },
    },
  },
};

// noinspection JSUnusedGlobalSymbols
export const CreateNewIncome = () => ({
  components: { EditIncome },
  template: '<EditIncome />',
  beforeCreate() {
    mockApiResources();
  },
});

// noinspection JSUnusedGlobalSymbols
export const CreateFromInvoice = () => ({
  components: { EditIncome },
  data() {
    return {
      invoice: {
        id: 100,
        title: 'Invoice #22041',
        currency: 'EUR',
        amount: 4276,
        generalTax: 100,
      },
    };
  },
  template: '<EditIncome :invoice="invoice" />',
  beforeCreate() {
    mockApiResources();
  },
});

// noinspection JSUnusedGlobalSymbols
export const EditExistingIncome = () => ({
  components: { EditIncome },
  template: '<EditIncome :id="42" />',
  beforeCreate() {
    mockApiResources();
    onGetToWorkspacePath('/incomes/42')
      .successJson(incomeProto);
  },
});

// noinspection JSUnusedGlobalSymbols
export const EditExistingIncomeWithInvoice = () => ({
  components: { EditIncome },
  template: '<EditIncome :id="42" />',
  beforeCreate() {
    mockApiResources();
    onGetToWorkspacePath('/incomes/42')
      .successJson({
        ...incomeProto,
        linkedInvoice: 100,
      });
    onGetToWorkspacePath('/invoices/100')
      .successJson({
        title: 'Invoice 994',
        dateIssued: '2020-05-03',
        currency: 'AUD',
        amount: 4276,
        id: 100,
      });
  },
});
