import { app as storybookApp } from '@storybook/vue3';
import { setupElPlus } from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
import { decoratorFactory } from '@/__storybook__/decorators/decorator-utils';

setupElPlus(storybookApp);

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
