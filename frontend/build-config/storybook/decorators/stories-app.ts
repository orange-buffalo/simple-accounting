// import { advanceTo, clear } from 'jest-date-mock';
import type { Story, StoryContext } from '@storybook/vue3';
import { app as storybookApp } from '@storybook/vue3';
// @ts-ignore
import mainConfig from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
// @ts-ignore
import { i18nPlugin } from '@/setup/i18n-plugin';
// init client
import '@/services/api';

mainConfig.setupApp();

export const {
  router,
} = mainConfig.app;
export const { app } = mainConfig;

storybookApp.use(i18nPlugin);
storybookApp.use(router);

export function createStoriesAppDecorator() {
  return (fn: Story, { parameters }: StoryContext) => ({
    components: { StoriesApp },
    data() {
      // if (parameters.skipMockTime) {
      //   clear();
      // } else {
      //   advanceTo(new Date('2030-01-04T00:00:00'));
      // }

      return {
        fullScreen: parameters.fullScreen,
      };
    },
    template: '<StoriesApp :full-screen="fullScreen"><story /></StoriesApp>',
  });
}
