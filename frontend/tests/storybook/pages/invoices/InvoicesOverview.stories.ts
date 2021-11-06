import { Page } from 'puppeteer';
import InvoicesOverview from '@/views/invoices/InvoicesOverview';
import { CustomerDto, InvoiceDto } from '@/services/api';
import { apiPage, onGetToWorkspacePath } from '../../utils/stories-api-mocks';
import { setViewportHeight } from '../../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Invoices/InvoicesOverview',
  parameters: {
    fullWidth: true,
    skipMockTime: true,
    storyshots: {
      async setup(page: Page) {
        await setViewportHeight(page, 400);
        await page.waitForSelector('.overview-item__panel');
      },
    },
  },
};

function mockApiResponses() {
  onGetToWorkspacePath('/invoices')
    .successJson(apiPage([{
      title: 'Invoice #22041',
      customer: 77,
      timeRecorded: '2020-01-04T00:00:00',
      dateIssued: '2020-05-03',
      dueDate: '2030-01-01',
      currency: 'AUD',
      amount: 4276,
      status: 'PAID',
      attachments: [],
      id: 42,
      version: 0,
      datePaid: null,
      dateSent: null,
      notes: null,
      generalTax: null,
    } as InvoiceDto]));
  onGetToWorkspacePath('/customers')
    .successJson(apiPage([{
      id: 77,
      name: 'Favourite Customer Ltd',
    } as CustomerDto]));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([]));
}

export const Default = () => ({
  components: { InvoicesOverview },
  template: '<InvoicesOverview />',
  beforeCreate() {
    mockApiResponses();
  },
});

export const ReadOnly = () => ({
  components: { InvoicesOverview },
  template: '<InvoicesOverview />',
  beforeCreate() {
    mockApiResponses();
  },
});
ReadOnly.parameters = {
  workspace: {
    editable: false,
  },
};
