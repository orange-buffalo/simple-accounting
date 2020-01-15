import Vue from 'vue';
import Router from 'vue-router';
import router from './routes-definitions';
import { api } from '@/services/api';
import { SUCCESSFUL_LOGIN_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
import { app } from '@/services/app-services';

export default function setupRouter() {
  Vue.use(Router);

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

  return router;
}
