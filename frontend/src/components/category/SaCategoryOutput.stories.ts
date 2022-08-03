// noinspection JSUnusedGlobalSymbols

import { fetchMock, defaultWorkspacePath, neverEndingGetRequest } from '@/__storybook__/api-mocks';
import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { disableOutputLoaderAnimations, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaCategoryOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaCategoryOutput },
  template: '<SaCategoryOutput :category-id="42"/>',
  beforeCreate() {
    fetchMock.get(`path:${defaultWorkspacePath('/categories')}`, {
      data: [{
        id: 42,
        name: 'Category 42',
      }],
    });
  },
}), {
  screenshotPreparation: waitForText('Category 42'),
});

export const Loading = defineStory(() => ({
  components: { SaCategoryOutput },
  template: '<SaCategoryOutput :category-id="42"/>',
  beforeCreate() {
    fetchMock.get(`path:${defaultWorkspacePath('/categories')}`, {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableOutputLoaderAnimations(),
});
