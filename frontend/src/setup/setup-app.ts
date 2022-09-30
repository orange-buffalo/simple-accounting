import {
  ElButton, ElDatePicker, ElInput, ElFormItem, ElCheckbox, ElForm, ElPagination,
  ElTooltip, ElMain, ElContainer, ElAside, ElOption, ElOptionGroup, ElSelect,
  ElProgress, ElLoadingDirective, ElAlert, ElSwitch,
} from 'element-plus';
import type { App } from 'vue';
import { createApp } from 'vue';
import type { Router } from 'vue-router';
import SimpleAccounting from '@/SimpleAccounting.vue';
import setupRouter from '@/setup/setup-router';
import '@/styles/main.scss';

export { setLocaleFromProfile, setLocaleFromBrowser } from '@/services/i18n';

let vueApp: App | null = null;

let vueRouter: Router | null = null;

export function setupElPlus(app: App) {
  app.component('ElButton', ElButton);
  app.component('ElDatePicker', ElDatePicker);
  app.component('ElInput', ElInput);
  app.component('ElFormItem', ElFormItem);
  app.component('ElCheckbox', ElCheckbox);
  app.component('ElForm', ElForm);
  app.component('ElPagination', ElPagination);
  app.component('ElTooltip', ElTooltip);
  app.component('ElMain', ElMain);
  app.component('ElContainer', ElContainer);
  app.component('ElAside', ElAside);
  app.component('ElOption', ElOption);
  app.component('ElOptionGroup', ElOptionGroup);
  app.component('ElSelect', ElSelect);
  app.component('ElProgress', ElProgress);
  app.component('ElAlert', ElAlert);
  app.component('ElSwitch', ElSwitch);

  app.directive('loading', ElLoadingDirective);
}

export function setupApp() {
  vueApp = createApp(SimpleAccounting);

  vueRouter = setupRouter();
  vueApp.use(vueRouter);

  setupElPlus(vueApp);
}

export function mountApp() {
  if (!vueApp) throw new Error('Vue app was not setup');
  vueApp.mount('#simple-accounting');
}

export function router(): Router {
  if (!vueRouter) throw Error('Vue app was not setup');
  return vueRouter;
}
