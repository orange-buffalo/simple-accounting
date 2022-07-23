import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/pages/login/Login.vue';
import AuthenticatedPage from '@/components/authenticated-page/AuthenticatedPage.vue';
// import { SUCCESSFUL_LOGIN_EVENT, LOGIN_REQUIRED_EVENT } from '@/services/events';
// import { useLastView } from '@/services/use-last-view';
// import router from './routes-definitions';

// function setupAuthenticationHooks() {
//   const {
//     isLoggedIn,
//     tryAutoLogin,
//   } = useAuth();
//   router.beforeEach(async (to, from, next) => {
//     const { setLastView } = useLastView();
//     if (to.name !== 'login'
//       && to.name !== 'logout'
//       && to.name !== 'login-by-link'
//       && to.name !== 'oauth-callback'
//       && !isLoggedIn()) {
//       if (await tryAutoLogin()) {
//         SUCCESSFUL_LOGIN_EVENT.emit();
//         next();
//       } else {
//         setLastView(to.name);
//         next({ name: 'login' });
//       }
//     } else {
//       next();
//     }
//   });
//
//   LOGIN_REQUIRED_EVENT.subscribe(() => router.push('/login'));
// }

export default function setupRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/login',
        name: 'login',
        component: Login,
      },

      // {
      //   path: '/login-by-link/:token',
      //   name: 'login-by-link',
      //   component: LoginByLink,
      //   props: true,
      // },
      //
      // {
      //   path: '/workspace-setup',
      //   name: 'workspace-setup',
      //   component: WorkspaceSetup,
      // },
      //
      // {
      //   path: '/logout',
      //   name: 'logout',
      //   beforeEnter: (to, from, next) => {
      //     api.logout()
      //       .then(() => next('login'));
      //   },
      // },
      //
      // {
      //   path: '/oauth-callback',
      //   name: 'oauth-callback',
      //   component: OAuthCallbackPage,
      // },
      //
      {
        path: '/',
        component: AuthenticatedPage,
        children: [
      //     {
      //       path: '',
      //       name: 'dashboard',
      //       component: Dashboard,
      //     },
      //     {
      //       path: 'settings/categories',
      //       name: 'settings-categories',
      //       component: Categories,
      //     },
      //     {
      //       path: 'settings/categories/create',
      //       name: 'create-new-category',
      //       component: CreateCategory,
      //     },
      //     {
      //       path: 'expenses',
      //       name: 'expenses-overview',
      //       component: ExpensesOverview,
      //     },
      //     {
      //       path: 'expenses/create',
      //       name: 'create-new-expense',
      //       props: true,
      //       component: EditExpense,
      //     },
      //     {
      //       path: 'expenses/:id/edit',
      //       name: 'edit-expense',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditExpense,
      //     },
      //     {
      //       path: 'incomes',
      //       name: 'incomes-overview',
      //       component: IncomesOverview,
      //     },
      //     {
      //       path: 'incomes/create',
      //       name: 'create-new-income',
      //       component: EditIncome,
      //       props: true,
      //     },
      //     {
      //       path: 'incomes/:id/edit',
      //       name: 'edit-income',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditIncome,
      //     },
      //     {
      //       path: 'invoices',
      //       name: 'invoices-overview',
      //       component: InvoicesOverview,
      //     },
      //     {
      //       path: 'invoices/create',
      //       name: 'create-new-invoice',
      //       component: EditInvoice,
      //     },
      //     {
      //       path: 'invoices/:id/edit',
      //       name: 'edit-invoice',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditInvoice,
      //     },
      //     {
      //       path: 'income-tax-payments',
      //       name: 'income-tax-payments-overview',
      //       component: IncomeTaxPaymentsOverview,
      //     },
      //     {
      //       path: 'income-tax-payments/create',
      //       name: 'create-new-income-tax-payment',
      //       component: EditIncomeTaxPayment,
      //     },
      //     {
      //       path: 'income-tax-payments/:id/edit',
      //       name: 'edit-income-tax-payment',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditIncomeTaxPayment,
      //     },
      //     {
      //       path: 'settings/customers',
      //       name: 'customers-overview',
      //       component: CustomersOverview,
      //     },
      //     {
      //       path: 'settings/customers/create',
      //       name: 'create-new-customer',
      //       component: EditCustomer,
      //     },
      //     {
      //       path: 'setting/customers/:id/edit',
      //       name: 'edit-customer',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditCustomer,
      //     },
      //     {
      //       path: 'settings/general-taxes',
      //       name: 'general-taxes-overview',
      //       component: GeneralTaxesOverview,
      //     },
      //     {
      //       path: 'settings/general-taxes/create',
      //       name: 'create-new-general-tax',
      //       component: EditGeneralTax,
      //     },
      //     {
      //       path: 'setting/general-taxes/:id/edit',
      //       name: 'edit-general-tax',
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //       component: EditGeneralTax,
      //     },
      //     {
      //       path: 'settings/workspaces',
      //       name: 'workspaces-overview',
      //       component: TheWorkspacesOverview,
      //     },
      //     {
      //       path: 'settings/workspaces/create',
      //       name: 'create-new-workspace',
      //       component: TheWorkspaceEditor,
      //     },
      //     {
      //       path: 'settings/workspaces/:id/edit',
      //       name: 'edit-workspace',
      //       component: TheWorkspaceEditor,
      //       props: ID_ROUTER_PARAM_PROCESSOR,
      //     },
      //     {
      //       path: 'my-profile',
      //       name: 'my-profile',
      //       component: MyProfile,
      //     },
      //     {
      //       path: 'reporting',
      //       name: 'reporting',
      //       component: Reporting,
      //     },
      //     {
      //       path: 'admin/users',
      //       name: 'users-overview',
      //       component: UsersOverview,
      //     },
      //     {
      //       path: 'admin/users/create',
      //       name: 'create-new-user',
      //       component: CreateUser,
      //     },
        ],
      },
      //
      // {
      //   path: '*',
      //   redirect: {
      //     name: 'dashboard',
      //   },
      // },
    ],
  });

  // setupAuthenticationHooks();
  return router;
}
