import Vue from 'vue'
import Router from 'vue-router'
import Home from './views/Home'
import UserApp from './views/UserApp'
import Login from './views/Login'

Vue.use(Router)

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL + 'app/',
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login
    },

    {
      path: '/',
      component: UserApp,
      children: [
        {
          path: '',
          component: Home
        },
        {
          path: '*',
          redirect: '/'
        }
      ]
    }
  ]
})
