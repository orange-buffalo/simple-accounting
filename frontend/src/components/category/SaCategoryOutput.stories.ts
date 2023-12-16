// noinspection JSUnusedGlobalSymbols

import { neverEndingGetRequest, onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/Domain/Category/SaCategoryOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaCategoryOutput },
  template: '<SaCategoryOutput :category-id="storybookData.categories.planetExpressCategory.id"/>',
  ...storybookData.storyComponentConfig,
}), {
  screenshotPreparation: waitForText(storybookData.categories.planetExpressCategory.name),
});

export const Loading = defineStory(() => ({
  components: { SaCategoryOutput },
  template: '<SaCategoryOutput :category-id="42"/>',
  setup() {
    onGetToDefaultWorkspacePath('/categories', {}, neverEndingGetRequest);
  },
}));
