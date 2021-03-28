import Vue from 'vue';
import Router from 'vue-router';
import { api } from '@/services/api-legacy';
import { SUCCESSFUL_LOGIN_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
import { app } from '@/services/app-services';
import router from './routes-definitions';

function setupAuthenticationHooks() {
  router.beforeEach(async (to, from, next) => {
    if (to.name !== 'login'
      && to.name !== 'logout'
      && to.name !== 'login-by-link'
      && to.name !== 'oauth-callback'
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
  setupAuthenticationHooks();
  return router;
}
