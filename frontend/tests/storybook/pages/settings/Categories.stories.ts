import Categories from '@/views/settings/categories/Categories';
import { CategoryDto } from '@/services/api';
import { Page } from 'puppeteer';
import { apiPage, onGet } from '../../utils/stories-api-mocks';
import { setViewportHeight } from '../../utils/stories-utils';

export default {
  title: 'Pages/Settings/Categories',
  parameters: {
    fullWidth: true,
  },
};

export const Default = () => ({
  components: { Categories },
  template: '<Categories/>',
  beforeCreate() {
    onGet('/api/workspaces/42/categories')
      .successJson(apiPage<CategoryDto>([{
        id: 1,
        description: 'Category 1 description',
        expense: true,
        income: false,
        name: 'Category 1',
        version: 1,
      },
      {
        id: 2,
        description: 'Category 2 description',
        expense: true,
        income: true,
        name: 'Category 2',
        version: 1,
      }]));
  },
});
Default.parameters = {
  storyshots: {
    async setup(page: Page) {
      await setViewportHeight(page, 800);
    },
  },
};
