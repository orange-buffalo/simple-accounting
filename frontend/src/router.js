import Router from 'vue-router'
import Dashboard from './views/Dashboard'
import App from './views/App'
import Login from './views/Login'
import WorkspaceSetup from './views/WorkspaceSetup'
import {api} from '@/services/api'
import LoginByLink from '@/views/LoginByLink'

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login
    },

    {
      path: '/login-by-link/:token',
      name: 'login-by-link',
      component: LoginByLink,
      props: true
    },

    {
      path: '/workspace-setup',
      name: 'workspace-setup',
      component: WorkspaceSetup
    },

    {
      path: '/logout',
      name: 'logout',
      beforeEnter: (to, from, next) => {
        api.logout().then(() => next('login'))
      }
    },

    {
      path: '/',
      component: App,
      children: [
        {
          path: '',
          component: Dashboard
        },
        {
          path: 'settings/categories',
          name: 'settings-categories',
          component: () => import(/* webpackChunkName: "settings-categories" */ './views/settings/categories/Categories.vue')
        },
        {
          path: 'settings/categories/create',
          name: 'create-new-category',
          component: () => import(/* webpackChunkName: "create-new-category" */ './views/settings/categories/CreateCategory.vue')
        },
        {
          path: 'expenses',
          name: 'expenses-overview',
          component: () => import(/* webpackChunkName: "expenses-overview" */ './views/expenses/ExpensesOverview.vue')
        },
        {
          path: 'expenses/create',
          name: 'create-new-expense',
          props:true,
          component: () => import(/* webpackChunkName: "create-new-expense" */ './views/expenses/EditExpense.vue')
        },
        {
          path: 'expenses/:id/edit',
          name: 'edit-expense',
          props:true,
          component: () => import(/* webpackChunkName: "edit-expense" */ './views/expenses/EditExpense.vue')
        },
        {
          path: 'incomes',
          name: 'incomes-overview',
          component: () => import(/* webpackChunkName: "incomes-overview" */ './views/incomes/IncomesOverview.vue')
        },
        {
          path: 'incomes/create',
          name: 'create-new-income',
          component: () => import(/* webpackChunkName: "create-new-income" */ './views/incomes/EditIncome.vue')
        },
        {
          path: 'incomes/:id/edit',
          name: 'edit-income',
          component: () => import(/* webpackChunkName: "edit-income" */ './views/incomes/EditIncome.vue')
        },
        {
          path: 'invoices',
          name: 'invoices-overview',
          component: () => import(/* webpackChunkName: "invoices-overview" */ './views/invoices/InvoicesOverview.vue')
        },
        {
          path: 'invoices/create',
          name: 'create-new-invoice',
          component: () => import(/* webpackChunkName: "create-new-invoice" */ './views/invoices/EditInvoice.vue')
        },
        {
          path: 'invoices/:id/edit',
          name: 'edit-invoice',
          component: () => import(/* webpackChunkName: "edit-invoice" */ './views/invoices/EditInvoice.vue')
        },
        {
          path: 'tax-payments',
          name: 'tax-payments-overview',
          component: () => import(/* webpackChunkName: "tax-payments-overview" */ './views/tax-payments/TaxPaymentsOverview.vue')
        },
        {
          path: 'tax-payments/create',
          name: 'create-new-tax-payment',
          component: () => import(/* webpackChunkName: "create-new-tax-payment" */ './views/tax-payments/EditTaxPayment.vue')
        },
        {
          path: 'tax-payments/:id/edit',
          name: 'edit-tax-payment',
          //todo #88: should we use the same chunk name for edit and create as view is the same?
          component: () => import(/* webpackChunkName: "edit-tax-payment" */ './views/tax-payments/EditTaxPayment.vue')
        },
        {
          path: 'settings/customers',
          name: 'customers-overview',
          component: () => import(/* webpackChunkName: "customers-overview" */ './views/settings/customers/CustomersOverview.vue')
        },
        {
          path: 'settings/customers/create',
          name: 'create-new-customer',
          component: () => import(/* webpackChunkName: "create-new-customer" */ './views/settings/customers/EditCustomer.vue')
        },
        {
          path: 'setting/customers/:id/edit',
          name: 'edit-customer',
          component: () => import(/* webpackChunkName: "edit-customer" */ './views/settings/customers/EditCustomer.vue')
        },
        {
          path: 'settings/taxes',
          name: 'taxes-overview',
          component: () => import(/* webpackChunkName: "taxes-overview" */ './views/settings/taxes/TaxesOverview.vue')
        },
        {
          path: 'settings/taxes/create',
          name: 'create-new-tax',
          component: () => import(/* webpackChunkName: "create-new-tax" */ './views/settings/taxes/EditTax.vue')
        },
        {
          path: 'setting/taxes/:id/edit',
          name: 'edit-tax',
          component: () => import(/* webpackChunkName: "edit-tax" */ './views/settings/taxes/EditTax.vue')
        },
        {
          path: 'settings/workspaces',
          name: 'workspaces-overview',
          component: () => import(/* webpackChunkName: "workspaces-overview" */ './views/settings/workspaces/TheWorkspacesOverview.vue')
        },
        {
          path: 'settings/workspaces/create',
          name: 'create-new-workspace',
          component: () => import(/* webpackChunkName: "create-new-workspace" */ './views/settings/workspaces/TheWorkspaceEditor.vue')
        },
        {
          path: 'settings/workspaces/:id/edit',
          name: 'edit-workspace',
          component: () => import(/* webpackChunkName: "edit-workspace" */ './views/settings/workspaces/TheWorkspaceEditor.vue')
        },
        {
          path: 'my-profile',
          name: 'my-profile',
          component: () => import(/* webpackChunkName: "my-profile" */ './views/profile/MyProfile.vue')
        },
        {
          path: 'reporting',
          name: 'reporting',
          component: () => import(/* webpackChunkName: "reporting" */ './views/reporting/Reporting.vue')
        },
        {
          path: 'admin/users',
          name: 'users-overview',
          component: () => import(/* webpackChunkName: "users-overview" */ './views/admin/users/UsersOverview.vue')
        },
        {
          path: 'admin/users/create',
          name: 'create-new-user',
          component: () => import(/* webpackChunkName: "create-new-user" */ './views/admin/users/CreateUser.vue')
        },
        {
          path: '*',
          redirect: '/'
        }
      ]
    }
  ]
})
