import { createApp } from 'vue';
import SimpleAccounting from '@/SimpleAccounting.vue';
import setupRouter from '@/setup/setup-router';
import { app, AppServices } from '@/services/app-services';

function setupApp() {
  const vue = createApp(SimpleAccounting);

  const router = setupRouter();
  vue.use(router);

  app.init(
    vue,
    router,
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
