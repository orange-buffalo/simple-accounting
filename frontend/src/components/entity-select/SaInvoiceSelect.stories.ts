// noinspection JSUnusedGlobalSymbols

import { ref } from 'vue';
import { storybookData } from '@/__storybook__/storybook-data';
import type { ApiPage, InvoiceDto } from '@/services/api';
import SaInvoiceSelect from '@/components/entity-select/SaInvoiceSelect.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, openSelectDropdown } from '@/__storybook__/screenshots';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';

export default {
  title: 'Components/Domain/SaInvoiceSelect',
};

const invoices: Array<InvoiceDto> = [];
for (let i = 0; i < 15; i += 1) {
  invoices.push({
    id: i,
    title: `Invoice #000${i}`,
    customer: storybookData.customers.governmentOfEarth.id,
    timeRecorded: new Date('2020-01-04T00:00:00'),
    dateIssued: new Date('2020-05-03'),
    dueDate: new Date('2030-01-01'),
    currency: 'AUD',
    amount: 4276 + i * 23,
    status: 'PAID',
    attachments: [],
    version: 0,
  });
}

export const Default = defineStory(() => ({
  components: { SaInvoiceSelect },
  setup: () => ({
    emptySelectedInvoice: ref<number | undefined>(),
    prefilledSelectedInvoice: ref<number>(1),
  }),
  beforeCreate() {
    onGetToDefaultWorkspacePath('/invoices', {
      data: invoices.slice(0, 10),
      pageNumber: 1,
      totalElements: invoices.length,
      pageSize: 10,
    } as ApiPage<InvoiceDto>);
    onGetToDefaultWorkspacePath('/invoices/1', invoices[1]);
  },
  template: `
    <h3>Empty value</h3>
    <SaInvoiceSelect
      style="width: 400px"
      v-model="emptySelectedInvoice"
    />
    <br /> Selected invoice: {{emptySelectedInvoice}}

    <h3>Pre-selected value</h3>
    <SaInvoiceSelect
      style="width: 400px"
      v-model="prefilledSelectedInvoice"
    />
    <br /> Selected invoice: {{prefilledSelectedInvoice}}
  `,
}), {
  screenshotPreparation: allOf(
    openSelectDropdown('.el-select'),
    // cannot open dropdown due to https://github.com/element-plus/element-plus/issues/10103
    // waitForText('Entity 0'),
  ),
  useRealTime: true,
});
