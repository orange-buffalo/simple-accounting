import EditIncome from '@/views/incomes/EditIncome';
import { apiPage, onGet, onGetToWorkspacePath } from '@/stories/utils/stories-api-mocks';

const category = {
  id: 13,
  version: 0,
  name: 'Category 1',
  income: true,
  expense: true,
};

function mockApiResources() {
  onGetToWorkspacePath('/categories')
    .successJson(apiPage([category]));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([]));
  onGetToWorkspacePath('/statistics/currencies-shortlist')
    .successJson(['AUD', 'USD']);
  onGet('api/profile/documents-storage')
    .successJson({ active: true });
}

export default {
  title: 'Pages/Incomes/EditIncome',
  parameters: {
    fullWidth: true,
  },
};

export const CreateNewIncome = () => ({
  components: { EditIncome },
  template: '<EditIncome />',
  beforeCreate() {
    mockApiResources();
  },
});

export const EditExistingIncome = () => ({
  components: { EditIncome },
  template: '<EditIncome :id="42" />',
  beforeCreate() {
    mockApiResources();
    onGetToWorkspacePath('/incomes/42')
      .successJson({
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
      });
  },
});
