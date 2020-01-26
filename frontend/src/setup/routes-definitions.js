import Router from 'vue-router';
import { api } from '@/services/api';
import Login from '@/views/Login';
import LoginByLink from '@/views/LoginByLink';
import WorkspaceSetup from '@/views/WorkspaceSetup';
import TheAuthenticatedPage from '@/components/TheAuthenticatedPage';
import Dashboard from '@/views/Dashboard';
import Categories from '@/views/settings/categories/Categories';
import CreateCategory from '@/views/settings/categories/CreateCategory';
import ExpensesOverview from '@/views/expenses/ExpensesOverview';
import EditExpense from '@/views/expenses/EditExpense';
import IncomesOverview from '@/views/incomes/IncomesOverview';
import EditIncome from '@/views/incomes/EditIncome';
import InvoicesOverview from '@/views/invoices/InvoicesOverview';
import EditInvoice from '@/views/invoices/EditInvoice';
import IncomeTaxPaymentsOverview from '@/views/income-tax-payments/IncomeTaxPaymentsOverview';
import EditIncomeTaxPayment from '@/views/income-tax-payments/EditIncomeTaxPayment';
import CustomersOverview from '@/views/settings/customers/CustomersOverview';
import EditCustomer from '@/views/settings/customers/EditCustomer';
import GeneralTaxesOverview from '@/views/settings/general-taxes/GeneralTaxesOverview';
import EditGeneralTax from '@/views/settings/general-taxes/EditGeneralTax';
import TheWorkspacesOverview from '@/views/settings/workspaces/TheWorkspacesOverview';
import TheWorkspaceEditor from '@/views/settings/workspaces/TheWorkspaceEditor';
import MyProfile from '@/views/profile/MyProfile';
import Reporting from '@/views/reporting/Reporting';
import UsersOverview from '@/views/admin/users/UsersOverview';
import CreateUser from '@/views/admin/users/CreateUser';
import { ID_ROUTER_PARAM_PROCESSOR } from '@/components/utils/utils';

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
    },

    {
      path: '/login-by-link/:token',
      name: 'login-by-link',
      component: LoginByLink,
      props: true,
    },

    {
      path: '/workspace-setup',
      name: 'workspace-setup',
      component: WorkspaceSetup,
    },

    {
      path: '/logout',
      name: 'logout',
      beforeEnter: (to, from, next) => {
        api.logout()
          .then(() => next('login'));
      },
    },

    {
      path: '/',
      component: TheAuthenticatedPage,
      children: [
        {
          path: '',
          component: Dashboard,
        },
        {
          path: 'settings/categories',
          name: 'settings-categories',
          component: Categories,
        },
        {
          path: 'settings/categories/create',
          name: 'create-new-category',
          component: CreateCategory,
        },
        {
          path: 'expenses',
          name: 'expenses-overview',
          component: ExpensesOverview,
        },
        {
          path: 'expenses/create',
          name: 'create-new-expense',
          props: true,
          component: EditExpense,
        },
        {
          path: 'expenses/:id/edit',
          name: 'edit-expense',
          props: true,
          component: EditExpense,
        },
        {
          path: 'incomes',
          name: 'incomes-overview',
          component: IncomesOverview,
        },
        {
          path: 'incomes/create',
          name: 'create-new-income',
          component: EditIncome,
        },
        {
          path: 'incomes/:id/edit',
          name: 'edit-income',
          component: EditIncome,
        },
        {
          path: 'invoices',
          name: 'invoices-overview',
          component: InvoicesOverview,
        },
        {
          path: 'invoices/create',
          name: 'create-new-invoice',
          component: EditInvoice,
        },
        {
          path: 'invoices/:id/edit',
          name: 'edit-invoice',
          component: EditInvoice,
        },
        {
          path: 'income-tax-payments',
          name: 'income-tax-payments-overview',
          component: IncomeTaxPaymentsOverview,
        },
        {
          path: 'income-tax-payments/create',
          name: 'create-new-income-tax-payment',
          component: EditIncomeTaxPayment,
        },
        {
          path: 'income-tax-payments/:id/edit',
          name: 'edit-income-tax-payment',
          component: EditIncomeTaxPayment,
        },
        {
          path: 'settings/customers',
          name: 'customers-overview',
          component: CustomersOverview,
        },
        {
          path: 'settings/customers/create',
          name: 'create-new-customer',
          component: EditCustomer,
        },
        {
          path: 'setting/customers/:id/edit',
          name: 'edit-customer',
          component: EditCustomer,
          props: ID_ROUTER_PARAM_PROCESSOR,
        },
        {
          path: 'settings/general-taxes',
          name: 'general-taxes-overview',
          component: GeneralTaxesOverview,
        },
        {
          path: 'settings/general-taxes/create',
          name: 'create-new-general-tax',
          component: EditGeneralTax,
        },
        {
          path: 'setting/general-taxes/:id/edit',
          name: 'edit-general-tax',
          component: EditGeneralTax,
        },
        {
          path: 'settings/workspaces',
          name: 'workspaces-overview',
          component: TheWorkspacesOverview,
        },
        {
          path: 'settings/workspaces/create',
          name: 'create-new-workspace',
          component: TheWorkspaceEditor,
        },
        {
          path: 'settings/workspaces/:id/edit',
          name: 'edit-workspace',
          component: TheWorkspaceEditor,
        },
        {
          path: 'my-profile',
          name: 'my-profile',
          component: MyProfile,
        },
        {
          path: 'reporting',
          name: 'reporting',
          component: Reporting,
        },
        {
          path: 'admin/users',
          name: 'users-overview',
          component: UsersOverview,
        },
        {
          path: 'admin/users/create',
          name: 'create-new-user',
          component: CreateUser,
        },
        {
          path: '*',
          redirect: '/',
        },
      ],
    },
  ],
});
