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
import OAuthIdentityCallbackPage from '@/pages/oauth-identity-callback/OAuthIdentityCallbackPage.vue';
import IncomesOverview from '@/pages/incomes/IncomesOverview.vue';
import EditIncome from '@/pages/incomes/EditIncome.vue';
import IncomeTaxPaymentsOverview from '@/pages/income-tax-payments/IncomeTaxPaymentsOverview.vue';
import EditIncomeTaxPayment from '@/pages/income-tax-payments/EditIncomeTaxPayment.vue';
import Reporting from '@/pages/reporting/Reporting.vue';
import Integrations from '@/pages/integrations/Integrations.vue';
import WiseSetup from '@/pages/integrations/WiseSetup.vue';
import DocumentsOverview from '@/pages/documents/DocumentsOverview.vue';
import AccountStatementsOverview from '@/pages/account-statements/AccountStatementsOverview.vue';
import DocumentsMigration from '@/pages/documents/DocumentsMigration.vue';
import EditStandaloneDocument from '@/pages/documents/EditStandaloneDocument.vue';
import Categories from '@/pages/settings/categories/Categories.vue';
import EditCategory from '@/pages/settings/categories/EditCategory.vue';
import CustomersOverview from '@/pages/settings/customers/CustomersOverview.vue';
import EditCustomer from '@/pages/settings/customers/EditCustomer.vue';
import GeneralTaxesOverview from '@/pages/settings/general-taxes/GeneralTaxesOverview.vue';
import EditGeneralTax from '@/pages/settings/general-taxes/EditGeneralTax.vue';
import WorkspacesOverview from '@/pages/settings/workspaces/WorkspacesOverview.vue';
import WorkspaceEditor from '@/pages/settings/workspaces/WorkspaceEditor.vue';
import WorkspaceAccessTokens from '@/pages/settings/workspaces/WorkspaceAccessTokens.vue';
import LoginByLink from '@/pages/LoginByLink.vue';
import { useAuth } from '@/services/api';
import { useLastView } from '@/services/use-last-view';
import { LOGIN_REQUIRED_EVENT, SUCCESSFUL_LOGIN_EVENT } from '@/services/events';
import UsersOverview from '@/pages/admin/users/UsersOverview.vue';
import AccountActivationPage from '@/pages/account-activation/AccountActivationPage.vue';
import SaUnauthenticatedPage from '@/components/unauthenticated-page/SaUnauthenticatedPage.vue';
import EditUser from '@/pages/admin/users/EditUser.vue';
import OAuthProvidersOverview from '@/pages/admin/oauth-providers/OAuthProvidersOverview.vue';
import EditOAuthProvider from '@/pages/admin/oauth-providers/EditOAuthProvider.vue';
import AccountSetupPage from '@/pages/account-setup/AccountSetupPage.vue';
import { resetDocumentsStorageStatus } from '@/components/documents/storage/useDocumentsStorageStatus';
import { ROUTE_NAMES } from '@/services/route-names';

const ID_ROUTER_PARAM_PROCESSOR = (route: RouteLocation) => ({ id: route.params.id as string });
const PROTOTYPE_ROUTER_PARAM_PROCESSOR = (route: RouteLocation) => {
  const result: { prototype?: string } = {};
  if (route.params.prototype) {
    result.prototype = route.params.prototype as string;
  }
  return result;
};
const SOURCE_INVOICE_ID_ROUTER_PARAM_PROCESSOR = (route: RouteLocation) => {
  const result: { sourceInvoiceId?: string } = {};
  if (route.params.sourceInvoiceId) {
    result.sourceInvoiceId = route.params.sourceInvoiceId as string;
  }
  return result;
};

const ANONYMOUS_PAGES: Array<RouteRecordSingleView> = [
  {
    path: '/activate-account/:token',
    name: ROUTE_NAMES.activateAccount,
    component: AccountActivationPage,
    props: true,
    meta: {
      pathPrefix: '/activate-account',
    },
  },
  {
    path: '/login-by-link/:token',
    name: ROUTE_NAMES.loginByLink,
    component: LoginByLink,
    props: true,
    meta: {
      pathPrefix: '/login-by-link',
    },
  },
];

const ANONYMOUS_PAGES_NAMES = ANONYMOUS_PAGES.map((page) => page.name);

/**
 * Full screen pages that are rendered outside of the standard layouts, but still must be
 * reachable without an active session.
 */
const STANDALONE_ANONYMOUS_PATH_PREFIXES = ['/oauth-identity-callback'];

export const ANONYMOUS_PAGES_PATH_PREFIXES = [
  ...ANONYMOUS_PAGES
    .map((page) => page.meta?.pathPrefix)
    .filter((prefix) => prefix) as string[],
  ...STANDALONE_ANONYMOUS_PATH_PREFIXES,
];

function setupAuthenticationHooks(router: Router) {
  const {
    isLoggedIn,
    tryAutoLogin,
  } = useAuth();
  router.beforeEach(async (to, _, next) => {
    resetDocumentsStorageStatus();

    const { setLastView } = useLastView();
    if (to.name !== ROUTE_NAMES.login
      && to.name !== ROUTE_NAMES.oauthCallback
      && to.name !== ROUTE_NAMES.oauthIdentityCallback
      && !ANONYMOUS_PAGES_NAMES.includes(to.name)
      && !isLoggedIn()) {
      if (await tryAutoLogin()) {
        SUCCESSFUL_LOGIN_EVENT.emit();
        next();
      } else {
        setLastView(to.name);
        next({ name: ROUTE_NAMES.login });
      }
    } else {
      next();
    }
  });

  LOGIN_REQUIRED_EVENT.subscribe(() => router.push({ name: ROUTE_NAMES.login }));
}

export default function setupRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/login',
        name: ROUTE_NAMES.login,
        component: Login,
      },
      {
        path: '/account-setup',
        name: ROUTE_NAMES.accountSetup,
        component: AccountSetupPage,
      },
      {
        path: '/oauth-callback',
        name: ROUTE_NAMES.oauthCallback,
        component: OAuthCallbackPage,
      },
      {
        path: '/oauth-identity-callback',
        name: ROUTE_NAMES.oauthIdentityCallback,
        component: OAuthIdentityCallbackPage,
      },
      {
        path: '/',
        component: SaAuthenticatedPage,
        children: [
          {
            path: '',
            name: ROUTE_NAMES.dashboard,
            component: Dashboard,
          },
          {
            path: 'settings/categories',
            name: ROUTE_NAMES.settingsCategories,
            component: Categories,
          },
          {
            path: 'settings/categories/create',
            name: ROUTE_NAMES.createNewCategory,
            component: EditCategory,
          },
          {
            path: 'settings/categories/:id/edit',
            name: ROUTE_NAMES.editCategory,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditCategory,
          },
          {
            path: 'expenses',
            name: ROUTE_NAMES.expensesOverview,
            component: ExpensesOverview,
          },
          {
            path: 'expenses/create/:prototype?',
            name: ROUTE_NAMES.createNewExpense,
            props: PROTOTYPE_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'expenses/:id/edit',
            name: ROUTE_NAMES.editExpense,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'incomes',
            name: ROUTE_NAMES.incomesOverview,
            component: IncomesOverview,
          },
          {
            path: 'incomes/create/:sourceInvoiceId?',
            name: ROUTE_NAMES.createNewIncome,
            component: EditIncome,
            props: SOURCE_INVOICE_ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'incomes/:id/edit',
            name: ROUTE_NAMES.editIncome,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditIncome,
          },
          {
            path: 'invoices',
            name: ROUTE_NAMES.invoicesOverview,
            component: InvoicesOverview,
          },
          {
            path: 'invoices/create',
            name: ROUTE_NAMES.createNewInvoice,
            component: EditInvoice,
          },
          {
            path: 'invoices/:id/edit',
            name: ROUTE_NAMES.editInvoice,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditInvoice,
          },
          {
            path: 'income-tax-payments',
            name: ROUTE_NAMES.incomeTaxPaymentsOverview,
            component: IncomeTaxPaymentsOverview,
          },
          {
            path: 'income-tax-payments/create',
            name: ROUTE_NAMES.createNewIncomeTaxPayment,
            component: EditIncomeTaxPayment,
          },
          {
            path: 'income-tax-payments/:id/edit',
            name: ROUTE_NAMES.editIncomeTaxPayment,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditIncomeTaxPayment,
          },
          {
            path: 'settings/customers',
            name: ROUTE_NAMES.customersOverview,
            component: CustomersOverview,
          },
          {
            path: 'settings/customers/create',
            name: ROUTE_NAMES.createNewCustomer,
            component: EditCustomer,
          },
          {
            path: 'settings/customers/:id/edit',
            name: ROUTE_NAMES.editCustomer,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditCustomer,
          },
          {
            path: 'settings/general-taxes',
            name: ROUTE_NAMES.generalTaxesOverview,
            component: GeneralTaxesOverview,
          },
          {
            path: 'settings/general-taxes/create',
            name: ROUTE_NAMES.createNewGeneralTax,
            component: EditGeneralTax,
          },
          {
            path: 'settings/general-taxes/:id/edit',
            name: ROUTE_NAMES.editGeneralTax,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditGeneralTax,
          },
          {
            path: 'settings/integrations',
            name: ROUTE_NAMES.integrations,
            component: Integrations,
          },
          {
            path: 'settings/integrations/wise/setup',
            name: ROUTE_NAMES.wiseSetup,
            component: WiseSetup,
          },
          {
            path: 'settings/workspaces',
            name: ROUTE_NAMES.workspacesOverview,
            component: WorkspacesOverview,
          },
          {
            path: 'settings/workspaces/create',
            name: ROUTE_NAMES.createNewWorkspace,
            component: WorkspaceEditor,
          },
          {
            path: 'settings/workspaces/:id/edit',
            name: ROUTE_NAMES.editWorkspace,
            component: WorkspaceEditor,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'settings/workspaces/:id/access-tokens',
            name: ROUTE_NAMES.workspaceAccessTokens,
            component: WorkspaceAccessTokens,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'my-profile',
            name: ROUTE_NAMES.myProfile,
            component: MyProfile,
          },
          {
            path: 'reporting',
            name: ROUTE_NAMES.reporting,
            component: Reporting,
          },
          {
            path: 'documents',
            name: ROUTE_NAMES.documentsOverview,
            component: DocumentsOverview,
          },
          {
            path: 'account-statements',
            name: ROUTE_NAMES.accountStatementsOverview,
            component: AccountStatementsOverview,
          },
          {
            path: 'documents/migration',
            name: ROUTE_NAMES.documentsMigration,
            component: DocumentsMigration,
          },
          {
            path: 'documents/create',
            name: ROUTE_NAMES.createStandaloneDocument,
            component: EditStandaloneDocument,
          },
          {
            path: 'documents/:id/edit',
            name: ROUTE_NAMES.editStandaloneDocument,
            component: EditStandaloneDocument,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'admin/users',
            name: ROUTE_NAMES.usersOverview,
            component: UsersOverview,
          },
          {
            path: 'admin/users/create',
            name: ROUTE_NAMES.createNewUser,
            component: EditUser,
          },
          {
            path: 'admin/users/:id/edit',
            name: ROUTE_NAMES.editUser,
            component: EditUser,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'admin/oauth-providers',
            name: ROUTE_NAMES.oauthProvidersOverview,
            component: OAuthProvidersOverview,
          },
          {
            path: 'admin/oauth-providers/create',
            name: ROUTE_NAMES.createNewOauthProvider,
            component: EditOAuthProvider,
          },
          {
            path: 'admin/oauth-providers/:id/edit',
            name: ROUTE_NAMES.editOauthProvider,
            component: EditOAuthProvider,
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
          name: ROUTE_NAMES.dashboard,
        },
      },
    ],
  });

  setupAuthenticationHooks(router);

  return router;
}
