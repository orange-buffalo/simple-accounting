// noinspection JSUnusedGlobalSymbols

import type { GeneralTaxReportDto } from 'src/services/api';
import GeneralTaxReport from '@/pages/reporting/GeneralTaxReport.vue';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForOutputLoadersToLoad } from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Pages/Reporting/GeneralTaxReport',
};

export const Default = defineStory(() => ({
  components: { GeneralTaxReport },
  template: '<GeneralTaxReport :date-range="[new Date(\'2021-01-01\'), new Date(\'2021-12-31\')]" />',
  beforeCreate() {
    storybookData.mockApi();
    onGetToDefaultWorkspacePath('/reporting/general-taxes', {
      finalizedCollectedTaxes: [{
        includedItemsAmount: 31322,
        includedItemsNumber: 5,
        tax: storybookData.generalTaxes.planetExpressTax.id,
        taxAmount: 4324,
      }, {
        includedItemsAmount: 33242,
        includedItemsNumber: 1,
        tax: storybookData.generalTaxes.warTax.id,
        taxAmount: 4321,
      }],
      finalizedPaidTaxes: [{
        taxAmount: 3433,
        tax: storybookData.generalTaxes.planetExpressTax.id,
        includedItemsNumber: 3,
        includedItemsAmount: 948493,
      }],
      pendingCollectedTaxes: [{
        includedItemsNumber: 2,
        tax: storybookData.generalTaxes.planetExpressTax.id,
      }, {
        includedItemsNumber: 1,
        tax: storybookData.generalTaxes.warTax.id,
      }],
      pendingPaidTaxes: [{
        tax: storybookData.generalTaxes.warTax.id,
        includedItemsNumber: 1,
      }],
    } as GeneralTaxReportDto);
  },
}), {
  screenshotPreparation: allOf(
    waitForOutputLoadersToLoad(),
  ),
});
