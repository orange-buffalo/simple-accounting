// noinspection JSUnusedGlobalSymbols

import type { GeneralTaxReportDto } from 'src/services/api';
import Reporting from '@/pages/reporting/Reporting.vue';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Reporting/Reporting',
};

export const Default = defineStory(() => ({
  components: { Reporting },
  template: '<Reporting />',
  setup() {
    storybookData.mockApi();
    onGetToDefaultWorkspacePath('/reporting/general-taxes', {
      finalizedCollectedTaxes: [{
        includedItemsAmount: 31322,
        includedItemsNumber: 5,
        tax: storybookData.generalTaxes.planetExpressTax.id,
        taxAmount: 4324,
      }],
      finalizedPaidTaxes: [],
      pendingCollectedTaxes: [],
      pendingPaidTaxes: [],
    } as GeneralTaxReportDto);
  },
}), {
  asPage: true,
});
