import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import {initApi, LOGIN_REQUIRED_EVENT, SUCCESSFUL_LOGIN_EVENT} from '@/services/api'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import EventBus from 'eventbusjs'
import Router from 'vue-router'
import AppLayout from '@/app/components/AppLayout'

Vue.config.productionTip = false

Vue.use(ElementUI);
Vue.component('app-layout', AppLayout);

router.beforeEach((to, from, next) => {
  if (to.name !== 'login' && !store.getters['api/isLoggedIn']) {
    next({name: 'login'})
  }
  else {
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

EventBus.addEventListener(LOGIN_REQUIRED_EVENT, () => {
  router.push('/login')
})

EventBus.addEventListener(SUCCESSFUL_LOGIN_EVENT, () => {
  store.dispatch('workspaces/loadWorkspaces').then(() => {
    if (!store.state.workspaces.currentWorkspace) {
      router.push('/workspace-setup')
    }
    else {
      router.push('/')
    }
  })
})