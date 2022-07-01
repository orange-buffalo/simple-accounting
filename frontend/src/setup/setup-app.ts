import { createApp } from 'vue';
import SimpleAccounting from '@/SimpleAccounting.vue';
import setupRouter from '@/setup/setup-router';
import { app, AppServices } from '@/services/app-services';
import { i18n } from '@/services/i18n';
import { i18nPlugin } from '@/setup/i18n-plugin';
import '@/styles/main.scss';

function setupApp() {
  const vue = createApp(SimpleAccounting);

  const router = setupRouter();
  vue.use(router);
  vue.use(i18nPlugin);

  app.init(
    vue,
    router,
    i18n,
  );
}

function mountApp() {
  app.vue.mount('#simple-accounting');
}

export interface SimpleAccountingInitializer {
  setupApp: () => void;
  app: AppServices;
  mountApp: () => void;
}

const initializer: SimpleAccountingInitializer = {
  app,
  mountApp,
  setupApp,
};

export default initializer;
