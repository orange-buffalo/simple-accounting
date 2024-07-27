import qs from 'qs';
import {
  AuthenticationApiApi,
  Configuration,
  ProfileApiControllerApi,
  WorkspacesApiApi,
  CustomersApiApi,
  CategoriesApiApi,
  StatisticsApiControllerApi,
  InvoicesApiApi,
  GeneralTaxesApiApi,
  DocumentsApiApi,
  GoogleDriveStorageApiControllerApi,
  OAuth2CallbackApiApi,
  ExpensesApiApi,
  IncomesApiApi,
  IncomeTaxPaymentsApiApi,
  ReportingApiControllerApi,
  WorkspaceAccessTokensApiApi,
  UsersApiApi,
  UserActivationTokensApiApi,
} from '@/services/api/generated';
import type { ConfigurationParameters } from '@/services/api/generated';
import { requestTimeoutInterceptor } from '@/services/api/interceptors/timeout-interceptor';
import { loadingEventsInterceptor } from '@/services/api/interceptors/loading-interceptor';
import { authorizationTokenInterceptor, expiredTokenInterceptor } from '@/services/api/interceptors/auth-interceptor';
import { errorHandlingInterceptor } from '@/services/api/interceptors/error-handling-interceptor';

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

export const authApi = new AuthenticationApiApi(new Configuration({
  ...defaultConfigParameters,
  middleware: [
    requestTimeoutInterceptor,
    loadingEventsInterceptor,
    authorizationTokenInterceptor,
    errorHandlingInterceptor,
  ],
}));

export const workspacesApi = new WorkspacesApiApi(defaultConfig);
export const profileApi = new ProfileApiControllerApi(defaultConfig);
export const customersApi = new CustomersApiApi(defaultConfig);
export const categoriesApi = new CategoriesApiApi(defaultConfig);
export const statisticsApi = new StatisticsApiControllerApi(defaultConfig);
export const invoicesApi = new InvoicesApiApi(defaultConfig);
export const expensesApi = new ExpensesApiApi(defaultConfig);
export const incomesApi = new IncomesApiApi(defaultConfig);
export const documentsApi = new DocumentsApiApi(defaultConfig);
export const generalTaxesApi = new GeneralTaxesApiApi(defaultConfig);
export const googleDriveStorageApi = new GoogleDriveStorageApiControllerApi(defaultConfig);
export const oAuth2CallbackApi = new OAuth2CallbackApiApi(defaultConfig);
export const incomeTaxPaymentsApi = new IncomeTaxPaymentsApiApi(defaultConfig);
export const reportingApi = new ReportingApiControllerApi(defaultConfig);
export const workspaceAccessTokensApi = new WorkspaceAccessTokensApiApi(defaultConfig);
export const usersApi = new UsersApiApi(defaultConfig);
export const userActivationTokensApi = new UserActivationTokensApiApi(defaultConfig);
