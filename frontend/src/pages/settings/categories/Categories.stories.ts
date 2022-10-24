// noinspection JSUnusedGlobalSymbols

import type { ApiPage, CategoryDto } from 'src/services/api';
import Categories from '@/pages/settings/categories/Categories.vue';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Pages/Settings/Categories',
};

export const Default = defineStory(() => ({
  components: { Categories },
  template: '<Categories />',
  beforeCreate() {
    onGetToDefaultWorkspacePath('/categories', {
      pageSize: 10,
      totalElements: 2,
      pageNumber: 1,
      data: [storybookData.categories.slurmCategory, storybookData.categories.planetExpressCategory],
    } as ApiPage<CategoryDto>);
  },
}), {
  asPage: true,
  useRealTime: true,
  screenshotPreparation: waitForText(storybookData.categories.planetExpressCategory.name),
});
