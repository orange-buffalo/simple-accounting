import { app as storybookApp } from '@storybook/vue3';
import { router, setupApp } from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

// todo: probably mock router instead of setup?
setupApp();
storybookApp.use(router());

export const createStoriesAppDecorator = decoratorFactory((parameters) => ({
  components: { StoriesApp },
  data() {
    return {
      fullScreen: parameters.fullScreen,
    };
  },
  template: '<StoriesApp :full-screen="fullScreen"><story /></StoriesApp>',
}));
