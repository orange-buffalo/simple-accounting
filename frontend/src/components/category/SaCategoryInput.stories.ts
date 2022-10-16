// noinspection JSUnusedGlobalSymbols

import {
  neverEndingGetRequest,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import SaCategoryInput from '@/components/category/SaCategoryInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  allOf, openSelectDropdown, waitForElementToBeVisible,
} from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/Domain/Category/SaCategoryInput',
};

export const Default = defineStory(() => ({
  components: { SaCategoryInput },
  data: () => ({
    presetCategoryId: storybookData.categories.planetExpressCategory.id,
    initiallyEmptyCategoryId: undefined,
  }),
  template: `
    <h4>Empty value</h4>
    <SaCategoryInput v-model="initiallyEmptyCategoryId"
                     placeholder="Please select category"
                     clearable
                     id="initially-empty-select"
    />
    <br />
    {{ initiallyEmptyCategoryId }}

    <h4>Preset value</h4>
    <SaCategoryInput v-model="presetCategoryId"
                     id="preset-select"
    />
    <br />
    {{ presetCategoryId }}
  `,
  ...storybookData.storyComponentConfig,
}), {
  screenshotPreparation: allOf(
    waitForElementToBeVisible('#preset-select > .el-select'),
    openSelectDropdown('#initially-empty-select > .el-select'),
    waitForElementToBeVisible(storybookData.categories.slurmCategory.name),
  ),
});

export const Loading = defineStory(() => ({
  components: { SaCategoryInput },
  template: '<SaCategoryInput />',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/categories', {}, neverEndingGetRequest);
  },
}));
