import type { App } from 'vue';
import { createApp } from 'vue';
import type { Router } from 'vue-router';
import vSelect from 'vue-select';
import SimpleAccounting from '@/SimpleAccounting.vue';
import setupRouter from '@/setup/setup-router';
import '@/styles/main.scss';

export { setLocaleFromProfile, setLocaleFromBrowser } from '@/services/i18n';

let vueApp: App | null = null;

let vueRouter: Router | null = null;

export function setupApp() {
  vueApp = createApp(SimpleAccounting);

  vueRouter = setupRouter();
  vueApp.use(vueRouter);
  vueApp.component('ElSelect', vSelect);
}

export function mountApp() {
  if (!vueApp) throw new Error('Vue app was not setup');
  vueApp.mount('#simple-accounting');
}

export function router(): Router {
  if (!vueRouter) throw Error('Vue app was not setup');
  return vueRouter;
}
