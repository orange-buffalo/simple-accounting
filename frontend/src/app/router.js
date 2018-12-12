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
          component: () => import(/* webpackChunkName: "create-new-expense" */ './views/expenses/EditExpense.vue')
        },
        {
          path: 'expenses/:id/edit',
          name: 'edit-expense',
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
          //todo should we use the same chunk name for edit and create as view is the same?
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
          path: '*',
          redirect: '/'
        }
      ]
    }
  ]
})
