import mainConfig from '@/setup/setup-app';
import StoriesApp from './StoriesApp';

mainConfig.setupApp();

export const { i18n, store, router } = mainConfig.app;
export const { app } = mainConfig;

export function createStoriesAppDecorator() {
  return (fn, { parameters }) => ({
    components: { StoriesApp },
    data() {
      return { fullWidth: parameters.fullWidth };
    },
    template: '<StoriesApp :full-width="fullWidth"><story /></StoriesApp>',
    i18n,
    store,
  });
}
