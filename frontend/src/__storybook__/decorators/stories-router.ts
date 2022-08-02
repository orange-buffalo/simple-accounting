import { app as storybookApp } from '@storybook/vue3';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';
import { action } from '@storybook/addon-actions';
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

storybookApp.use(router);

export const createRouterDecorator = decoratorFactory(() => ({
    template: '<story/>',
  }));
