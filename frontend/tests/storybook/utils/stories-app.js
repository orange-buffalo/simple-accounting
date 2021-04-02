import { advanceTo } from 'jest-date-mock';
import mainConfig from '@/setup/setup-app';
import StoriesApp from './StoriesApp';
// init client
import '@/services/api';

advanceTo(new Date('2030-01-04T00:00:00'));

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
