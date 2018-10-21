import Router from 'vue-router'
import Home from './views/Home'
import UserApp from './views/UserApp'
import Login from './views/Login'
import WorkspaceSetup from './views/WorkspaceSetup'

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
      path: '/workspace-setup',
      name: 'workspace-setup',
      component: WorkspaceSetup
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
          path: 'settings/categories',
          component: () => import(/* webpackChunkName: "settings-categories" */ './views/settings/categories/Categories.vue')
        },
        {
          path: 'settings/categories/create',
          name: 'create-new-category',
          component: () => import(/* webpackChunkName: "settings-categories" */ './views/settings/categories/Categories.vue')
        },
        {
          path: '*',
          redirect: '/'
        }
      ]
    }
  ]
})
