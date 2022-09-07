// noinspection JSUnusedGlobalSymbols

import {
  neverEndingGetRequest,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, clickOnElement, waitForElementToBeVisible,
} from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/SaGeneralTaxInput',
};

export const Default = defineStory(() => ({
  components: { SaGeneralTaxInput },
  data: () => ({
    presetTaxId: storybookData.generalTaxes.planetExpressTax.id,
    initiallyEmptyTaxId: undefined,
  }),
  template: `
    <h4>Empty value</h4>
    <SaGeneralTaxInput v-model="initiallyEmptyTaxId"
                       placeholder="Please select tax"
                       clearable
                       id="initially-empty-select"
    />
    <br />
    {{ initiallyEmptyTaxId }}

    <h4>Preset value</h4>
    <SaGeneralTaxInput v-model="presetTaxId"
                       id="preset-select"
    />
    <br />
    {{ presetTaxId }}
  `,
  ...storybookData.storyComponentConfig,
}), {
  screenshotPreparation: allOf(
    waitForElementToBeVisible('#preset-select > .el-select'),
    clickOnElement('#initially-empty-select > .el-select'),
    waitForElementToBeVisible('.el-select-dropdown'),
  ),
});

export const Loading = defineStory(() => ({
  components: { SaGeneralTaxInput },
  template: '<SaGeneralTaxInput />',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/general-taxes', {}, neverEndingGetRequest);
  },
}));
