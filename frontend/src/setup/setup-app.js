import Vue from 'vue';
import ElementUI from 'element-ui';
import Main from '@/Main.vue';
// todo #6 move to main.vue
import '@/styles/main.scss';
import setupRouter from '@/setup/setup-router';
import setupStore from '@/setup/setup-store';
import setupI18n from '@/setup/setup-i18n';
import { app } from '@/services/app-services';

function setupApp() {
  Vue.config.productionTip = false;

  const router = setupRouter();
  const store = setupStore();
  const i18n = setupI18n();
  const vue = new Vue({
    router,
    store,
    i18n,
    render: h => h(Main),
  });

  Vue.use(ElementUI, {
    i18n: (key, value) => i18n.t(key, value),
  });

  app.init({
    vue,
    router,
    store,
    i18n,
  });
}

function mountApp() {
  app.vue.$mount('#app');
}

export default {
  app,
  setupApp,
  mountApp,
};
