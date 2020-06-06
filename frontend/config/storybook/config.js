// eslint-disable-next-line import/no-extraneous-dependencies
import { addDecorator } from '@storybook/vue';
// eslint-disable-next-line import/no-extraneous-dependencies
import centered from '@storybook/addon-centered/vue';
import mainConfig from '@/setup/setup-app';

mainConfig.setupApp();
// noinspection JSIgnoredPromiseFromCall
mainConfig.app.i18n.setLocaleFromProfile({
  locale: 'en',
  language: 'en',
});

addDecorator(centered);

addDecorator(() => ({
  template: '<story/>',
  i18n: mainConfig.app.i18n,
  store: mainConfig.app.store,
}));
