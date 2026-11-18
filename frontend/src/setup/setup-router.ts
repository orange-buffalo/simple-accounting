import type { RouteLocation, Router, RouteRecordSingleView } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';
import Login, { LOGIN_ROUTE_NAME } from '@/pages/login/Login.vue';
import SaAuthenticatedPage from '@/components/authenticated-page/SaAuthenticatedPage.vue';
import Dashboard, { DASHBOARD_ROUTE_NAME } from '@/pages/dashboard/Dashboard.vue';
import InvoicesOverview, { INVOICES_OVERVIEW_ROUTE_NAME } from '@/pages/invoices/InvoicesOverview.vue';
import EditInvoice, { CREATE_NEW_INVOICE_ROUTE_NAME, EDIT_INVOICE_ROUTE_NAME } from '@/pages/invoices/EditInvoice.vue';
import EditExpense, { CREATE_NEW_EXPENSE_ROUTE_NAME, EDIT_EXPENSE_ROUTE_NAME } from '@/pages/expenses/EditExpense.vue';
import ExpensesOverview, { EXPENSES_OVERVIEW_ROUTE_NAME } from '@/pages/expenses/ExpensesOverview.vue';
import MyProfile, { MY_PROFILE_ROUTE_NAME } from '@/pages/my-profile/MyProfile.vue';
import OAuthCallbackPage, { OAUTH_CALLBACK_ROUTE_NAME } from '@/pages/oauth-callback/OAuthCallbackPage.vue';
import OAuthIdentityCallbackPage, {
  OAUTH_IDENTITY_CALLBACK_ROUTE_NAME,
} from '@/pages/oauth-identity-callback/OAuthIdentityCallbackPage.vue';
import IncomesOverview, { INCOMES_OVERVIEW_ROUTE_NAME } from '@/pages/incomes/IncomesOverview.vue';
import EditIncome, { CREATE_NEW_INCOME_ROUTE_NAME, EDIT_INCOME_ROUTE_NAME } from '@/pages/incomes/EditIncome.vue';
import IncomeTaxPaymentsOverview, {
  INCOME_TAX_PAYMENTS_OVERVIEW_ROUTE_NAME,
} from '@/pages/income-tax-payments/IncomeTaxPaymentsOverview.vue';
import EditIncomeTaxPayment, {
  CREATE_NEW_INCOME_TAX_PAYMENT_ROUTE_NAME,
  EDIT_INCOME_TAX_PAYMENT_ROUTE_NAME,
} from '@/pages/income-tax-payments/EditIncomeTaxPayment.vue';
import Reporting, { REPORTING_ROUTE_NAME } from '@/pages/reporting/Reporting.vue';
import Integrations, { INTEGRATIONS_ROUTE_NAME } from '@/pages/integrations/Integrations.vue';
import WiseSetup, { WISE_SETUP_ROUTE_NAME } from '@/pages/integrations/WiseSetup.vue';
import DocumentsOverview, { DOCUMENTS_OVERVIEW_ROUTE_NAME } from '@/pages/documents/DocumentsOverview.vue';
import AccountStatementsOverview, {
  ACCOUNT_STATEMENTS_OVERVIEW_ROUTE_NAME,
} from '@/pages/account-statements/AccountStatementsOverview.vue';
import DocumentsMigration, { DOCUMENTS_MIGRATION_ROUTE_NAME } from '@/pages/documents/DocumentsMigration.vue';
import EditStandaloneDocument, {
  CREATE_STANDALONE_DOCUMENT_ROUTE_NAME,
  EDIT_STANDALONE_DOCUMENT_ROUTE_NAME,
} from '@/pages/documents/EditStandaloneDocument.vue';
import Categories, { SETTINGS_CATEGORIES_ROUTE_NAME } from '@/pages/settings/categories/Categories.vue';
import EditCategory, {
  CREATE_NEW_CATEGORY_ROUTE_NAME, EDIT_CATEGORY_ROUTE_NAME,
} from '@/pages/settings/categories/EditCategory.vue';
import CustomersOverview, { CUSTOMERS_OVERVIEW_ROUTE_NAME } from '@/pages/settings/customers/CustomersOverview.vue';
import EditCustomer, {
  CREATE_NEW_CUSTOMER_ROUTE_NAME, EDIT_CUSTOMER_ROUTE_NAME,
} from '@/pages/settings/customers/EditCustomer.vue';
import GeneralTaxesOverview, {
  GENERAL_TAXES_OVERVIEW_ROUTE_NAME,
} from '@/pages/settings/general-taxes/GeneralTaxesOverview.vue';
import EditGeneralTax, {
  CREATE_NEW_GENERAL_TAX_ROUTE_NAME, EDIT_GENERAL_TAX_ROUTE_NAME,
} from '@/pages/settings/general-taxes/EditGeneralTax.vue';
import WorkspacesOverview, { WORKSPACES_OVERVIEW_ROUTE_NAME } from '@/pages/settings/workspaces/WorkspacesOverview.vue';
import WorkspaceEditor, {
  CREATE_NEW_WORKSPACE_ROUTE_NAME, EDIT_WORKSPACE_ROUTE_NAME,
} from '@/pages/settings/workspaces/WorkspaceEditor.vue';
import WorkspaceAccessTokens, {
  WORKSPACE_ACCESS_TOKENS_ROUTE_NAME,
} from '@/pages/settings/workspaces/WorkspaceAccessTokens.vue';
import LoginByLink, { LOGIN_BY_LINK_ROUTE_NAME } from '@/pages/LoginByLink.vue';
import { useAuth } from '@/services/api';
import { useLastView } from '@/services/use-last-view';
import { LOGIN_REQUIRED_EVENT, SUCCESSFUL_LOGIN_EVENT } from '@/services/events';
import UsersOverview, { USERS_OVERVIEW_ROUTE_NAME } from '@/pages/admin/users/UsersOverview.vue';
import AccountActivationPage, {
  ACTIVATE_ACCOUNT_ROUTE_NAME,
} from '@/pages/account-activation/AccountActivationPage.vue';
import SaUnauthenticatedPage from '@/components/unauthenticated-page/SaUnauthenticatedPage.vue';
import EditUser, { CREATE_NEW_USER_ROUTE_NAME, EDIT_USER_ROUTE_NAME } from '@/pages/admin/users/EditUser.vue';
import OAuthProvidersOverview, {
  OAUTH_PROVIDERS_OVERVIEW_ROUTE_NAME,
} from '@/pages/admin/oauth-providers/OAuthProvidersOverview.vue';
import EditOAuthProvider, {
  CREATE_NEW_OAUTH_PROVIDER_ROUTE_NAME, EDIT_OAUTH_PROVIDER_ROUTE_NAME,
} from '@/pages/admin/oauth-providers/EditOAuthProvider.vue';
import AccountSetupPage, { ACCOUNT_SETUP_ROUTE_NAME } from '@/pages/account-setup/AccountSetupPage.vue';
import { resetDocumentsStorageStatus } from '@/components/documents/storage/useDocumentsStorageStatus';

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
    name: ACTIVATE_ACCOUNT_ROUTE_NAME,
    component: AccountActivationPage,
    props: true,
    meta: {
      pathPrefix: '/activate-account',
    },
  },
  {
    path: '/login-by-link/:token',
    name: LOGIN_BY_LINK_ROUTE_NAME,
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
    if (to.name !== LOGIN_ROUTE_NAME
      && to.name !== OAUTH_CALLBACK_ROUTE_NAME
      && to.name !== OAUTH_IDENTITY_CALLBACK_ROUTE_NAME
      && !ANONYMOUS_PAGES_NAMES.includes(to.name)
      && !isLoggedIn()) {
      if (await tryAutoLogin()) {
        SUCCESSFUL_LOGIN_EVENT.emit();
        next();
      } else {
        setLastView(to.name);
        next({ name: LOGIN_ROUTE_NAME });
      }
    } else {
      next();
    }
  });

  LOGIN_REQUIRED_EVENT.subscribe(() => router.push({ name: LOGIN_ROUTE_NAME }));
}

export default function setupRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/login',
        name: LOGIN_ROUTE_NAME,
        component: Login,
      },
      {
        path: '/account-setup',
        name: ACCOUNT_SETUP_ROUTE_NAME,
        component: AccountSetupPage,
      },
      {
        path: '/oauth-callback',
        name: OAUTH_CALLBACK_ROUTE_NAME,
        component: OAuthCallbackPage,
      },
      {
        path: '/oauth-identity-callback',
        name: OAUTH_IDENTITY_CALLBACK_ROUTE_NAME,
        component: OAuthIdentityCallbackPage,
      },
      {
        path: '/',
        component: SaAuthenticatedPage,
        children: [
          {
            path: '',
            name: DASHBOARD_ROUTE_NAME,
            component: Dashboard,
          },
          {
            path: 'settings/categories',
            name: SETTINGS_CATEGORIES_ROUTE_NAME,
            component: Categories,
          },
          {
            path: 'settings/categories/create',
            name: CREATE_NEW_CATEGORY_ROUTE_NAME,
            component: EditCategory,
          },
          {
            path: 'settings/categories/:id/edit',
            name: EDIT_CATEGORY_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditCategory,
          },
          {
            path: 'expenses',
            name: EXPENSES_OVERVIEW_ROUTE_NAME,
            component: ExpensesOverview,
          },
          {
            path: 'expenses/create/:prototype?',
            name: CREATE_NEW_EXPENSE_ROUTE_NAME,
            props: PROTOTYPE_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'expenses/:id/edit',
            name: EDIT_EXPENSE_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditExpense,
          },
          {
            path: 'incomes',
            name: INCOMES_OVERVIEW_ROUTE_NAME,
            component: IncomesOverview,
          },
          {
            path: 'incomes/create/:sourceInvoiceId?',
            name: CREATE_NEW_INCOME_ROUTE_NAME,
            component: EditIncome,
            props: SOURCE_INVOICE_ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'incomes/:id/edit',
            name: EDIT_INCOME_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditIncome,
          },
          {
            path: 'invoices',
            name: INVOICES_OVERVIEW_ROUTE_NAME,
            component: InvoicesOverview,
          },
          {
            path: 'invoices/create',
            name: CREATE_NEW_INVOICE_ROUTE_NAME,
            component: EditInvoice,
          },
          {
            path: 'invoices/:id/edit',
            name: EDIT_INVOICE_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditInvoice,
          },
          {
            path: 'income-tax-payments',
            name: INCOME_TAX_PAYMENTS_OVERVIEW_ROUTE_NAME,
            component: IncomeTaxPaymentsOverview,
          },
          {
            path: 'income-tax-payments/create',
            name: CREATE_NEW_INCOME_TAX_PAYMENT_ROUTE_NAME,
            component: EditIncomeTaxPayment,
          },
          {
            path: 'income-tax-payments/:id/edit',
            name: EDIT_INCOME_TAX_PAYMENT_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditIncomeTaxPayment,
          },
          {
            path: 'settings/customers',
            name: CUSTOMERS_OVERVIEW_ROUTE_NAME,
            component: CustomersOverview,
          },
          {
            path: 'settings/customers/create',
            name: CREATE_NEW_CUSTOMER_ROUTE_NAME,
            component: EditCustomer,
          },
          {
            path: 'settings/customers/:id/edit',
            name: EDIT_CUSTOMER_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditCustomer,
          },
          {
            path: 'settings/general-taxes',
            name: GENERAL_TAXES_OVERVIEW_ROUTE_NAME,
            component: GeneralTaxesOverview,
          },
          {
            path: 'settings/general-taxes/create',
            name: CREATE_NEW_GENERAL_TAX_ROUTE_NAME,
            component: EditGeneralTax,
          },
          {
            path: 'settings/general-taxes/:id/edit',
            name: EDIT_GENERAL_TAX_ROUTE_NAME,
            props: ID_ROUTER_PARAM_PROCESSOR,
            component: EditGeneralTax,
          },
          {
            path: 'settings/integrations',
            name: INTEGRATIONS_ROUTE_NAME,
            component: Integrations,
          },
          {
            path: 'settings/integrations/wise/setup',
            name: WISE_SETUP_ROUTE_NAME,
            component: WiseSetup,
          },
          {
            path: 'settings/workspaces',
            name: WORKSPACES_OVERVIEW_ROUTE_NAME,
            component: WorkspacesOverview,
          },
          {
            path: 'settings/workspaces/create',
            name: CREATE_NEW_WORKSPACE_ROUTE_NAME,
            component: WorkspaceEditor,
          },
          {
            path: 'settings/workspaces/:id/edit',
            name: EDIT_WORKSPACE_ROUTE_NAME,
            component: WorkspaceEditor,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'settings/workspaces/:id/access-tokens',
            name: WORKSPACE_ACCESS_TOKENS_ROUTE_NAME,
            component: WorkspaceAccessTokens,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'my-profile',
            name: MY_PROFILE_ROUTE_NAME,
            component: MyProfile,
          },
          {
            path: 'reporting',
            name: REPORTING_ROUTE_NAME,
            component: Reporting,
          },
          {
            path: 'documents',
            name: DOCUMENTS_OVERVIEW_ROUTE_NAME,
            component: DocumentsOverview,
          },
          {
            path: 'account-statements',
            name: ACCOUNT_STATEMENTS_OVERVIEW_ROUTE_NAME,
            component: AccountStatementsOverview,
          },
          {
            path: 'documents/migration',
            name: DOCUMENTS_MIGRATION_ROUTE_NAME,
            component: DocumentsMigration,
          },
          {
            path: 'documents/create',
            name: CREATE_STANDALONE_DOCUMENT_ROUTE_NAME,
            component: EditStandaloneDocument,
          },
          {
            path: 'documents/:id/edit',
            name: EDIT_STANDALONE_DOCUMENT_ROUTE_NAME,
            component: EditStandaloneDocument,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'admin/users',
            name: USERS_OVERVIEW_ROUTE_NAME,
            component: UsersOverview,
          },
          {
            path: 'admin/users/create',
            name: CREATE_NEW_USER_ROUTE_NAME,
            component: EditUser,
          },
          {
            path: 'admin/users/:id/edit',
            name: EDIT_USER_ROUTE_NAME,
            component: EditUser,
            props: ID_ROUTER_PARAM_PROCESSOR,
          },
          {
            path: 'admin/oauth-providers',
            name: OAUTH_PROVIDERS_OVERVIEW_ROUTE_NAME,
            component: OAuthProvidersOverview,
          },
          {
            path: 'admin/oauth-providers/create',
            name: CREATE_NEW_OAUTH_PROVIDER_ROUTE_NAME,
            component: EditOAuthProvider,
          },
          {
            path: 'admin/oauth-providers/:id/edit',
            name: EDIT_OAUTH_PROVIDER_ROUTE_NAME,
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
          name: DASHBOARD_ROUTE_NAME,
        },
      },
    ],
  });

  setupAuthenticationHooks(router);

  return router;
}
