import Vue from 'vue';
import ElementUI from 'element-ui';
import Main from './Main.vue';
import router from './router';
import store from './store';
import { api, initApi, LOGIN_REQUIRED_EVENT } from '@/services/api';
import '@/styles/main.scss';
import EventBus from 'eventbusjs';
import Router from 'vue-router';
import { setupApp } from '@/services/app-services';
import SvgIcon from 'vue-svgicon';
import { initPushNotifications } from '@/services/push-notifications';

Vue.config.productionTip = false;

Vue.use(ElementUI);
Vue.use(SvgIcon);

router.beforeEach(async (to, from, next) => {
  if (to.name !== 'login'
      && to.name !== 'logout'
      && to.name !== 'login-by-link'
      && !store.getters['api/isLoggedIn']) {
    if (await api.tryAutoLogin()) {
      await setupApp(store, router);
      next();
    } else {
      store.commit('app/setLastView', to.name);
      next({ name: 'login' });
    }
  } else {
    next();
  }
});
Vue.use(Router);

new Vue({
  router,
  store,
  render: h => h(Main),
}).$mount('#app');

initApi(store);
initPushNotifications(store);

// todo #6: calculate proper locale
store.dispatch('i18n/loadLocaleData');

EventBus.addEventListener(LOGIN_REQUIRED_EVENT, () => {
  router.push('/login');
});
