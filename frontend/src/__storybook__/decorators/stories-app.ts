import { setup } from '@storybook/vue3';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';
import { action } from '@storybook/addon-actions';
import setupErrorHandler from '@/setup/setup-error-handler';
import { setupComponents } from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

const EmptyView = defineComponent({
  name: 'RouterMockEmptyView',
  render: () => null,
});

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    {
      path: '/:pathMatch(.*)*',
      component: EmptyView,
    },
  ],
});
router.beforeEach((guard) => {
  action('router-navigation')(guard.path);
});

setup((app) => {
  setupComponents(app);
  setupErrorHandler(app);
  app.use(router);
});

export const createStoriesAppDecorator = decoratorFactory((parameters) => ({
  components: { StoriesApp },
  data() {
    return {
      fullScreen: parameters.fullScreen,
      asPage: parameters.asPage,
    };
  },
  template: '<StoriesApp :full-screen="fullScreen" :as-page="asPage"><story /></StoriesApp>',
}));
