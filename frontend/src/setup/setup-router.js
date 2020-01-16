import Vue from 'vue';
import Router from 'vue-router';
import router from './routes-definitions';
import { api } from '@/services/api';
import {
  SUCCESSFUL_LOGIN_EVENT,
  LOGIN_REQUIRED_EVENT,
  NAVIGATION_STARTED_EVENT,
  NAVIGATION_FINISHED_EVENT,
} from '@/services/events';
import { app } from '@/services/app-services';

function setupNavigationEventsHooks() {
  router.beforeEach((to, from, next) => {
    NAVIGATION_STARTED_EVENT.emit();
    next();
  });

  router.afterEach(() => NAVIGATION_FINISHED_EVENT.emit());
}

function setupAuthenticationHooks() {
  router.beforeEach(async (to, from, next) => {
    if (to.name !== 'login'
      && to.name !== 'logout'
      && to.name !== 'login-by-link'
      && !api.isLoggedIn()) {
      if (await api.tryAutoLogin()) {
        SUCCESSFUL_LOGIN_EVENT.emit();
        next();
      } else {
        app.store.commit('app/setLastView', to.name);
        next({ name: 'login' });
      }
    } else {
      next();
    }
  });

  LOGIN_REQUIRED_EVENT.subscribe(() => router.push('/login'));
}

export default function setupRouter() {
  Vue.use(Router);
  setupNavigationEventsHooks();
  setupAuthenticationHooks();
  return router;
}
