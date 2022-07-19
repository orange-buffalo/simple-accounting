// import { advanceTo, clear } from 'jest-date-mock';
import type { Story, StoryContext } from '@storybook/vue3';
import { app as storybookApp } from '@storybook/vue3';
// @ts-ignore
import { router, setupApp } from '@/setup/setup-app';
import StoriesApp from './StoriesApp.vue';
// init client
import '@/services/api';

// todo: probably mock router instead of setup?
setupApp();
storybookApp.use(router());

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
