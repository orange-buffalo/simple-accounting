import { advanceTo, clear } from 'jest-date-mock';
import mainConfig from '@/setup/setup-app';
import StoriesApp from './StoriesApp';
// init client
import '@/services/api';

mainConfig.setupApp();

export const {
  i18n,
  store,
  router,
} = mainConfig.app;
export const { app } = mainConfig;

export function createStoriesAppDecorator() {
  return (fn, { parameters }) => ({
    components: { StoriesApp },
    data() {
      if (parameters.skipMockTime) {
        clear();
      } else {
        advanceTo(new Date('2030-01-04T00:00:00'));
      }

      return {
        fullWidth: parameters.fullWidth,
        fullScreen: parameters.fullScreen,
      };
    },
    template: '<StoriesApp :full-width="fullWidth" :full-screen="fullScreen"><story /></StoriesApp>',
    i18n,
    store,
  });
}
