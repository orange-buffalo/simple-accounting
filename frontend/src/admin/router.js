import Vue from 'vue'
import Router from 'vue-router'
import Home from './views/Home'
import Admin from './views/Admin'
import Login from './views/Login'

Vue.use(Router)

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL + 'admin/',
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login
    },

    {
      path: '/',
      component: Admin,
      children: [
        {
          path: '',
          component: Home
        },
        {
          path: '/users',
          component: () => import(/* webpackChunkName: "users" */ './views/UsersOverview.vue')
        },

        {
          path: '*',
          redirect: '/'
        }
      ]
    }
  ]
})
