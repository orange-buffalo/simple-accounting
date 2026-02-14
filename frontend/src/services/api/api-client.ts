import qs from 'qs';
import type { ConfigurationParameters } from '@/services/api/generated';
import {
  AuthenticationApiApi,
  CategoriesApiApi,
  Configuration,
  CustomersApiApi,
  DocumentsApiApi,
  ExpensesApiApi,
  GeneralTaxesApiApi,
  GoogleDriveStorageApiApi,
  IncomesApiApi,
  IncomeTaxPaymentsApiApi,
  InvoicesApiApi,
  OAuth2CallbackApiApi,
  ProfileApiApi,
  ReportingApiApi,
  StatisticsApiApi,
  UserActivationTokensApiApi,
  UsersApiApi,
  WorkspaceAccessTokensApiApi,
  WorkspacesApiApi,
} from '@/services/api/generated';
import { authorizationTokenInterceptor, expiredTokenInterceptor } from '@/services/api/interceptors/auth-interceptor';
import { errorHandlingInterceptor } from '@/services/api/interceptors/error-handling-interceptor';
import { loadingEventsInterceptor } from '@/services/api/interceptors/loading-interceptor';
import { requestTimeoutInterceptor } from '@/services/api/interceptors/timeout-interceptor';

const defaultConfigParameters: ConfigurationParameters = {
  basePath: '',
  queryParamsStringify: (params) => qs.stringify(params, { arrayFormat: 'repeat' }),
  middleware: [
    requestTimeoutInterceptor,
    loadingEventsInterceptor,
    authorizationTokenInterceptor,
    expiredTokenInterceptor,
    errorHandlingInterceptor,
  ],
};
const defaultConfig = new Configuration(defaultConfigParameters);

export const authApi = new AuthenticationApiApi(
  new Configuration({
    ...defaultConfigParameters,
    middleware: [
      requestTimeoutInterceptor,
      loadingEventsInterceptor,
      authorizationTokenInterceptor,
      errorHandlingInterceptor,
    ],
  }),
);

export const workspacesApi = new WorkspacesApiApi(defaultConfig);
export const profileApi = new ProfileApiApi(defaultConfig);
export const customersApi = new CustomersApiApi(defaultConfig);
export const categoriesApi = new CategoriesApiApi(defaultConfig);
export const statisticsApi = new StatisticsApiApi(defaultConfig);
export const invoicesApi = new InvoicesApiApi(defaultConfig);
export const expensesApi = new ExpensesApiApi(defaultConfig);
export const incomesApi = new IncomesApiApi(defaultConfig);
export const documentsApi = new DocumentsApiApi(defaultConfig);
export const generalTaxesApi = new GeneralTaxesApiApi(defaultConfig);
export const googleDriveStorageApi = new GoogleDriveStorageApiApi(defaultConfig);
export const oAuth2CallbackApi = new OAuth2CallbackApiApi(defaultConfig);
export const incomeTaxPaymentsApi = new IncomeTaxPaymentsApiApi(defaultConfig);
export const reportingApi = new ReportingApiApi(defaultConfig);
export const workspaceAccessTokensApi = new WorkspaceAccessTokensApiApi(defaultConfig);
export const usersApi = new UsersApiApi(defaultConfig);
export const userActivationTokensApi = new UserActivationTokensApiApi(defaultConfig);
