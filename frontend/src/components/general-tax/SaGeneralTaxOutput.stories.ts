// noinspection JSUnusedGlobalSymbols

import {
  neverEndingGetRequest,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/Domain/GeneralTax/SaGeneralTaxOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaGeneralTaxOutput },
  ...storybookData.storyComponentConfig,
  template: '<SaGeneralTaxOutput :general-tax-id="storybookData.generalTaxes.planetExpressTax.id"/>',
}), {
  screenshotPreparation: waitForText(storybookData.generalTaxes.planetExpressTax.title),
});

export const Loading = defineStory(() => ({
  components: { SaGeneralTaxOutput },
  template: '<SaGeneralTaxOutput :general-tax-id="7"/>',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/general-taxes/7', {}, neverEndingGetRequest);
  },
}));
