import type { RouteLocation, Router, RouteRecordSingleView } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/pages/login/Login.vue';
import SaAuthenticatedPage from '@/components/authenticated-page/SaAuthenticatedPage.vue';
import Dashboard from '@/pages/dashboard/Dashboard.vue';
import InvoicesOverview from '@/pages/invoices/InvoicesOverview.vue';
import EditInvoice from '@/pages/invoices/EditInvoice.vue';
import EditExpense from '@/pages/expenses/EditExpense.vue';
import ExpensesOverview from '@/pages/expenses/ExpensesOverview.vue';
import MyProfile from '@/pages/my-profile/MyProfile.vue';
import OAuthCallbackPage from '@/pages/oauth-callback/OAuthCallbackPage.vue';
import IncomesOverview from '@/pages/incomes/IncomesOverview.vue';
import EditIncome from '@/pages/incomes/EditIncome.vue';
import IncomeTaxPaymentsOverview from '@/pages/income-tax-payments/IncomeTaxPaymentsOverview.vue';
import EditIncomeTaxPayment from '@/pages/income-tax-payments/EditIncomeTaxPayment.vue';
import Reporting from '@/pages/reporting/Reporting.vue';
import Categories from '@/pages/settings/categories/Categories.vue';
import CreateCategory from '@/pages/settings/categories/CreateCategory.vue';
import CustomersOverview from '@/pages/settings/customers/CustomersOverview.vue';
import EditCustomer from '@/pages/settings/customers/EditCustomer.vue';
import GeneralTaxesOverview from '@/pages/settings/general-taxes/GeneralTaxesOverview.vue';
import EditGeneralTax from '@/pages/settings/general-taxes/EditGeneralTax.vue';
import WorkspacesOverview from '@/pages/settings/workspaces/WorkspacesOverview.vue';
import WorkspaceEditor from '@/pages/settings/workspaces/WorkspaceEditor.vue';
import LoginByLink from '@/pages/LoginByLink.vue';
import { useAuth } from '@/services/api';
import { useLastView } from '@/services/use-last-view';
import { LOGIN_REQUIRED_EVENT, SUCCESSFUL_LOGIN_EVENT } from '@/services/events';
import UsersOverview from '@/pages/admin/users/UsersOverview.vue';
import AccountActivationPage from '@/pages/account-activation/AccountActivationPage.vue';
import SaUnauthenticatedPage from '@/components/unauthenticated-page/SaUnauthenticatedPage.vue';
import EditUser from '@/pages/admin/users/EditUser.vue';
import AccountSetupPage from '@/pages/account-setup/AccountSetupPage.vue';

const ID_ROUTER_PARAM_PROCESSOR = (route: RouteLocation) => ({ id: Number(route.params.id) });
const PROTOTYPE_ROUTER_PARAM_PROCESSOR = (route: RouteLocation) => {
  const result: { prototype?: string } = {};
  if (route.params.prototype) {
    result.prototype = route.params.prototype as string;
  }
  return result;
};

const ANONYMOUS_PAGES: Array<RouteRecordSingleView> = [
  {
    path: '/activate-account/:token',
    name: 'activate-account',
    component: AccountActivationPage,
    props: true,
    meta: {
      pathPrefix: '/activate-account',
    },
  }];

const ANONYMOUS_PAGES_NAMES = ANONYMOUS_PAGES.map((page) => page.name);
export const ANONYMOUS_PAGES_PATH_PREFIXES = ANONYMOUS_PAGES
  .map((page) => page.meta?.pathPrefix)
  .filter((prefix) => prefix) as string[];

function setupAuthenticationHooks(router: Router) {
  const {
    isLoggedIn,
    tryAutoLogin,
  } = useAuth();
  router.beforeEach(async (to, _, next) => {
    const { setLastView } = useLastView();
    // todo #117: remove from the list of explicit checks
    if (to.name !== 'login'
      && to.name !== 'login-by-link'
      && to.name !== 'oauth-callback'
      && !ANONYMOUS_PAGES_NAMES.includes(to.name)
      && !isLoggedIn()) {
      if (await tryAutoLogin()) {
        SUCCESSFUL_LOGIN_EVENT.emit();
        next();
      } else {
        setLastView(to.name);
        next({ name: 'login' });
      }
    } else {
      next();
    }
  });

  LOGIN_REQUIRED_EVENT.subscribe(() => router.push('/login'));
}

export default function setupRouter() {
  const router = createRouter({
    history: createWebHistory(),
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
        path: '/account-setup',
        name: 'account-setup',
        component: AccountSetupPage,
      },
      {
        path: '/oauth-callback',
        name: 'oauth-callback',
        component: OAuthCallbackPage,
      },
      {
        path: '/',
        component: SaAuthenticatedPage,
        children: [
          {
            path: '',
            name: 'dashboard',
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
            path: 'expenses/create/:prototype?',
            name: 'create-new-expense',
            props: PROTOTYPE_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'expenses/:id/edit',
            name: 'edit-expense',
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'incomes',
            name: 'incomes-overview',
            component: IncomesOverview,
          },
          {
            path: 'incomes/create/:sourceInvoiceId?',
            name: 'create-new-income',
            component: EditIncome,
            props: true,
          },
          {
            path: 'incomes/:id/edit',
            name: 'edit-income',
            props: ID_ROUTER_PARAM_PROCESSOR,
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
            props: ID_ROUTER_PARAM_PROCESSOR,
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
            props: ID_ROUTER_PARAM_PROCESSOR,
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
            path: 'settings/customers/:id/edit',
            name: 'edit-customer',
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditCustomer,
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
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditGeneralTax,
          },
          {
            path: 'settings/workspaces',
            name: 'workspaces-overview',
            component: WorkspacesOverview,
          },
          {
            path: 'settings/workspaces/create',
            name: 'create-new-workspace',
            component: WorkspaceEditor,
          },
          {
            path: 'settings/workspaces/:id/edit',
            name: 'edit-workspace',
            component: WorkspaceEditor,
            props: ID_ROUTER_PARAM_PROCESSOR,
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
            component: EditUser,
          },
          {
            path: 'admin/users/:id/edit',
            name: 'edit-user',
            component: EditUser,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
        ],
      },

      {
        path: '/',
        component: SaUnauthenticatedPage,
        children: [
          ...ANONYMOUS_PAGES,
        ],
      },

      {
        path: '/:catchAll(.*)',
        redirect: {
          name: 'dashboard',
        },
      },
    ],
  });

  setupAuthenticationHooks(router);

  return router;
}
