import { action } from '@storybook/addon-actions';
import SaInvoiceSelect from '@/components/invoice/SaInvoiceSelect';
import { onGetToWorkspacePath, apiPage } from '../utils/stories-api-mocks';
import { setViewportHeight, storyshotsStory, timeout } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components|SaInvoiceSelect',
};

async function toggleSelectInStoryshots(page) {
  const openIndicator = await page.$('.vs__open-indicator');
  await openIndicator.click();
  await timeout(1000);
}

export const Empty = createStory();
Empty.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
    await setViewportHeight(page, 500);
  },
});

export const PreSelected = createStory({
  data() {
    return {
      invoice: 42,
    };
  },
});
PreSelected.story = storyshotsStory({
  async setup(page) {
    await toggleSelectInStoryshots(page);
    await setViewportHeight(page, 500);
  },
});

function createStory(componentSpec) {
  return () => ({
    components: { SaInvoiceSelect },
    data() {
      return {
        invoice: null,
      };
    },
    watch: {
      invoice() {
        action('on-input')(this.invoice);
      },
    },
    template: '<SaInvoiceSelect v-model="invoice" />',
    beforeCreate() {
      mockInvoicesApi();
    },
    ...componentSpec,
  });
}

let nextInvoiceId = 33232;
let nextInvoiceAmount = 332945;

const invoicePrototype = {
  title: 'Invoice #22041',
  dateIssued: '2020-05-03',
  currency: 'AUD',
  amount: 4276,
  id: 0,
};

function creteInvoice(spec) {
  nextInvoiceId += 1;
  nextInvoiceAmount += nextInvoiceId / 2;
  return {
    ...invoicePrototype,
    id: nextInvoiceId,
    title: `Invoice #${nextInvoiceId}`,
    amount: nextInvoiceAmount,
    ...spec,
  };
}

const invoices = [];
for (let i = 0; i < 400; i += 1) {
  invoices.push(creteInvoice());
}

function mockInvoicesApi() {
  onGetToWorkspacePath('invoices')
    .intercept((req, res) => {
      let data = invoices;
      const search = req.query.freeSearchText;
      if (search) {
        data = data.filter((it) => it.title.includes(search.eq));
      }
      res.json(apiPage(data));
    });

  onGetToWorkspacePath('invoices/42')
    .successJson(creteInvoice({
      id: 42,
      title: 'Invoice #42',
    }));
}
