import { setupApp } from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

setupApp();

export const createStoriesAppDecorator = decoratorFactory((parameters) => ({
  components: { StoriesApp },
  data() {
    return {
      fullScreen: parameters.fullScreen,
    };
  },
  template: '<StoriesApp :full-screen="fullScreen"><story /></StoriesApp>',
}));
