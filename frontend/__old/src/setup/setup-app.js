import Vue from 'vue';
import ElementLocale from 'element-ui/lib/locale';
import VueCompositionApi from '@vue/composition-api';
import SimpleAccounting from '@/SimpleAccounting.vue';
import '@/styles/main.scss';
import setupRouter from '@/setup/setup-router';
import setupI18n from '@/setup/setup-i18n';
import setupErrorHandler from '@/setup/setup-error-handler';
import { app } from '@/services/app-services';

function setupElementUi({ i18n }) {
  ElementLocale.i18n((key, value) => i18n.t(key, value));
}

function setupApp() {
  Vue.config.productionTip = false;
  Vue.use(VueCompositionApi);

  const router = setupRouter();
  const i18n = setupI18n();
  const vue = new Vue({
    router,
    i18n,
    render: (h) => h(SimpleAccounting),
  });

  setupElementUi({ i18n });
  setupErrorHandler();

  app.init({
    vue,
    router,
    i18n,
  });
}

function mountApp() {
  app.vue.$mount('#simple-accounting');
}

export default {
  app,
  setupApp,
  mountApp,
};
