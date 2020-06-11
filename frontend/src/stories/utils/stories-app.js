import mainConfig from '@/setup/setup-app';
import StoriesApp from '@/stories/utils/StoriesApp';

mainConfig.setupApp();

export const { i18n, store } = mainConfig.app;
export const { app } = mainConfig;

export function createStoriesAppDecorator() {
  return () => ({
    components: { StoriesApp },
    template: '<StoriesApp><story /></StoriesApp>',
    i18n,
    store,
  });
}
