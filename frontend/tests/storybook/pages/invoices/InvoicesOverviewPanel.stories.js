import InvoicesOverviewPanel from '@/views/invoices/InvoicesOverviewPanel';
import { action } from '@storybook/addon-actions';
import { apiPage, onGetToWorkspacePath, onPutToWorkspacePath } from '../../utils/stories-api-mocks';
import { setViewportHeight, timeout } from '../../utils/stories-utils';

const customer = {
  id: 77,
  name: 'Favourite Customer Ltd',
};

function mockTaxesAndCustomers() {
  onGetToWorkspacePath('/customers')
    .successJson(apiPage([customer]));
  onGetToWorkspacePath('/general-taxes')
    .successJson(apiPage([]));
}

const invoicePrototype = {
  title: 'Invoice #22041',
  customer: customer.id,
  timeRecorded: '2020-01-04T00:00:00',
  dateIssued: '2020-05-03',
  dueDate: '2030-01-01',
  currency: 'AUD',
  amount: 4276,
  attachments: [],
  id: 42,
  version: 0,
};

function createStory({ invoice, componentConfig }) {
  return () => ({
    components: { InvoicesOverviewPanel },
    data() {
      return { invoice };
    },
    methods: {
      onInvoiceUpdate() {
        action('invoice-update')();
        this.invoice = {
          ...this.invoice,
          title: 'Updated Invoice',
          status: 'SENT',
        };
      },
    },
    template: '<InvoicesOverviewPanel :invoice="invoice" @invoice-update="onInvoiceUpdate" />',
    beforeCreate() {
      mockTaxesAndCustomers();
    },
    ...componentConfig,
  });
}

async function togglePanelInStoryshots(page) {
  const openIndicator = await page.$('.overview-item__details-trigger');
  await openIndicator.click();
  await timeout(500);
}

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages/Invoices/InvoicesOverviewPanel',
  parameters: {
    fullWidth: true,
  },
};

export const Draft = createStory({
  invoice: {
    ...invoicePrototype,
    status: 'DRAFT',
  },
  componentConfig: {
    beforeCreate() {
      mockTaxesAndCustomers();
      onPutToWorkspacePath(`invoices/${invoicePrototype.id}`)
        .intercept((req, res) => {
          action('PUT Invoice API')(req.body);
          res.json(req.body);
        });
    },
  },
});
Draft.parameters = {
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};

export const Sent = createStory({
  invoice: {
    ...invoicePrototype,
    dateSent: '2020-05-04',
    status: 'SENT',
  },
});
Sent.parameters = {
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};

export const Overdue = createStory({
  invoice: {
    ...invoicePrototype,
    dateSent: '2020-05-04',
    status: 'OVERDUE',
  },
});
Overdue.parameters = {
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};

export const Paid = createStory({
  invoice: {
    ...invoicePrototype,
    dateSent: '2020-05-04',
    datePaid: '2020-06-15',
    status: 'PAID',
  },
});
Paid.story.parameters = {
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};

export const Cancelled = createStory({
  invoice: {
    ...invoicePrototype,
    status: 'CANCELLED',
  },
});
Cancelled.parameters = {
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};

export const ReadOnlyWorkspace = createStory({
  invoice: {
    ...invoicePrototype,
    status: 'DRAFT',
  },
});
ReadOnlyWorkspace.parameters = {
  workspace: {
    editable: false,
  },
  storyshots: {
    async setup(page) {
      await togglePanelInStoryshots(page);
      await setViewportHeight(page, 450);
    },
  },
};
