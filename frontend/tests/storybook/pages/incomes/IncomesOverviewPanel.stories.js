import IncomesOverviewPanel from '@/views/incomes/IncomesOverviewPanel';
import { apiPage, onGetToWorkspacePath } from '../../utils/stories-api-mocks';

const incomePrototype = {
  title: 'Payment 343',
  timeRecorded: '2020-01-04T00:00:00',
  dateReceived: '2020-05-03',
  currency: 'AUD',
  originalAmount: 125686,
  attachments: [],
  id: 42,
  version: 0,
  status: 'FINALIZED',
  convertedAmounts: {
    originalAmountInDefaultCurrency: 125686,
    adjustedAmountInDefaultCurrency: 125686,
  },
  incomeTaxableAmounts: {
    originalAmountInDefaultCurrency: 125686,
    adjustedAmountInDefaultCurrency: 125686,
  },
  useDifferentExchangeRateForIncomeTaxPurposes: false,
};

function mockTaxesAndCategories() {
  onGetToWorkspacePath('/categories')
    .successJson(apiPage([]));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([]));
}

export default {
  title: 'Pages|Incomes/IncomesOverviewPanel',
  parameters: {
    fullWidth: true,
  },
};

export const WithInvoice = () => ({
  components: { IncomesOverviewPanel },
  data() {
    return {
      income: {
        ...incomePrototype,
        linkedInvoice: 100,
      },
    };
  },
  template: '<IncomesOverviewPanel :income="income" />',
  beforeCreate() {
    mockTaxesAndCategories();
    onGetToWorkspacePath('invoices/100')
      .successJson({
        title: 'Invoice #22041',
      });
  },
});

export const WithLoadingInvoice = () => ({
  components: { IncomesOverviewPanel },
  data() {
    return {
      income: {
        ...incomePrototype,
        linkedInvoice: 100,
      },
    };
  },
  template: '<IncomesOverviewPanel :income="income" />',
  beforeCreate() {
    mockTaxesAndCategories();
    onGetToWorkspacePath('invoices/100')
      .neverEndingRequest();
  },
});

export const Finalized = () => ({
  components: { IncomesOverviewPanel },
  data() {
    return {
      income: incomePrototype,
    };
  },
  template: '<IncomesOverviewPanel :income="income" />',
  beforeCreate() {
    mockTaxesAndCategories();
  },
});

export const PendingConversion = () => ({
  components: { IncomesOverviewPanel },
  data() {
    return {
      income: {
        ...incomePrototype,
        status: 'PENDING_CONVERSION',
        currency: 'USD',
        convertedAmounts: {},
        incomeTaxableAmounts: {},
      },
    };
  },
  template: '<IncomesOverviewPanel :income="income" />',
  beforeCreate() {
    mockTaxesAndCategories();
  },
});

export const PendingConversionForTaxation = () => ({
  components: { IncomesOverviewPanel },
  data() {
    return {
      income: {
        ...incomePrototype,
        status: 'PENDING_CONVERSION_FOR_TAXATION_PURPOSES',
        currency: 'USD',
        convertedAmounts: {
          originalAmountInDefaultCurrency: 754893,
          adjustedAmountInDefaultCurrency: 754893,
        },
        incomeTaxableAmounts: {},
        useDifferentExchangeRateForIncomeTaxPurposes: true,
      },
    };
  },
  template: '<IncomesOverviewPanel :income="income" />',
  beforeCreate() {
    mockTaxesAndCategories();
  },
});
