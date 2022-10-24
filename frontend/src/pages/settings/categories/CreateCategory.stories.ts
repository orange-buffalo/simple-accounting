// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import CreateCategory from '@/pages/settings/categories/CreateCategory.vue';
import { defaultWorkspacePath, fetchMock } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Pages/Settings/CreateCategory',
};

export const Default = defineStory(() => ({
  components: { CreateCategory },
  template: '<CreateCategory />',
  beforeCreate() {
    fetchMock.post(defaultWorkspacePath('/categories'), (_, req) => {
      action('POST /categories')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  useRealTime: true,
});
