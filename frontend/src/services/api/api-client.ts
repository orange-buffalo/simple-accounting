import qs from 'qs';
import {
  AuthenticationApiControllerApi,
  Configuration,
  ProfileApiControllerApi,
  WorkspacesApiControllerApi,
  CustomersApiControllerApi,
  CategoriesApiControllerApi,
  StatisticsApiControllerApi,
  InvoicesApiControllerApi,
  GeneralTaxApiControllerApi,
  DocumentsApiControllerApi,
  GoogleDriveStorageApiControllerApi,
  OAuth2CallbackControllerApi,
  ExpensesApiControllerApi,
  IncomesApiControllerApi,
  IncomeTaxPaymentsApiControllerApi,
  ReportingApiControllerApi,
  WorkspaceAccessTokensApiControllerApi,
  UsersApiControllerApi,
  UserActivationTokensApiControllerApi,
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

export const authApi = new AuthenticationApiControllerApi(new Configuration({
  ...defaultConfigParameters,
  middleware: [
    requestTimeoutInterceptor,
    loadingEventsInterceptor,
    authorizationTokenInterceptor,
    errorHandlingInterceptor,
  ],
}));

export const workspacesApi = new WorkspacesApiControllerApi(defaultConfig);
export const profileApi = new ProfileApiControllerApi(defaultConfig);
export const customersApi = new CustomersApiControllerApi(defaultConfig);
export const categoriesApi = new CategoriesApiControllerApi(defaultConfig);
export const statisticsApi = new StatisticsApiControllerApi(defaultConfig);
export const invoicesApi = new InvoicesApiControllerApi(defaultConfig);
export const expensesApi = new ExpensesApiControllerApi(defaultConfig);
export const incomesApi = new IncomesApiControllerApi(defaultConfig);
export const documentsApi = new DocumentsApiControllerApi(defaultConfig);
export const generalTaxesApi = new GeneralTaxApiControllerApi(defaultConfig);
export const googleDriveStorageApi = new GoogleDriveStorageApiControllerApi(defaultConfig);
export const oAuth2CallbackApi = new OAuth2CallbackControllerApi(defaultConfig);
export const incomeTaxPaymentsApi = new IncomeTaxPaymentsApiControllerApi(defaultConfig);
export const reportingApi = new ReportingApiControllerApi(defaultConfig);
export const workspaceAccessTokensApi = new WorkspaceAccessTokensApiControllerApi(defaultConfig);
export const usersApi = new UsersApiControllerApi(defaultConfig);
export const userActivationTokensApi = new UserActivationTokensApiControllerApi(defaultConfig);
