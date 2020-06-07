import mainConfig from '@/setup/setup-app';

mainConfig.setupApp();
// noinspection JSIgnoredPromiseFromCall
mainConfig.app.i18n.setLocaleFromProfile({
  locale: 'en',
  language: 'en',
});

export const { i18n, store } = mainConfig.app;
export const { app } = mainConfig;

export function createStoriesAppDecorator() {
  return () => ({
    template: '<story/>',
    i18n,
    store,
  });
}
