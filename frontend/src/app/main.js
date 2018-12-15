import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import {api, initApi, LOGIN_REQUIRED_EVENT} from '@/services/api'
import ElementUI from 'element-ui'
import './main.scss'
import EventBus from 'eventbusjs'
import Router from 'vue-router'
import {setupApp} from '@/app/services/app-services'

Vue.config.productionTip = false

Vue.use(ElementUI);

router.beforeEach(async (to, from, next) => {
  if (to.name !== 'login' && !store.getters['api/isLoggedIn']) {
    try {
      await api.tryAutoLogin()
      await setupApp(store, router)
      next()
    } catch (e) {
      store.commit('app/setLastView', to.name)
      next({name: 'login'})
    }
  } else {
    next()
  }
})
Vue.use(Router)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')

initApi(store)

//todo calculate proper locale
store.dispatch('i18n/loadLocaleData')

EventBus.addEventListener(LOGIN_REQUIRED_EVENT, () => {
  router.push('/login')
})