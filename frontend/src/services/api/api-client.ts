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
} from '@/services/api/generated';
import type { ConfigurationParameters } from '@/services/api/generated';
import { requestTimeoutInterceptor } from '@/services/api/interceptors/timeout-interceptor';
import { loadingEventsInterceptor } from '@/services/api/interceptors/loading-interceptor';
import { authorizationTokenInterceptor, expiredTokenInterceptor } from '@/services/api/interceptors/auth-interceptor';
import { errorHandlingInterceptor } from '@/services/api/interceptors/error-handling-interceptor';

export interface RequestMetadata {
  readonly skipGlobalErrorHandler?: boolean;
  readonly requestTimeoutMs?: number,
  requestTimeoutHandler?: ReturnType<typeof setTimeout>
}

const defaultConfigParameters: ConfigurationParameters<RequestMetadata> = {
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
const defaultConfig = new Configuration<RequestMetadata>(defaultConfigParameters);

export const authApi = new AuthenticationApiControllerApi<RequestMetadata>(new Configuration<RequestMetadata>({
  ...defaultConfigParameters,
  middleware: [
    requestTimeoutInterceptor,
    loadingEventsInterceptor,
    authorizationTokenInterceptor,
    errorHandlingInterceptor,
  ],
}));

export const workspacesApi = new WorkspacesApiControllerApi<RequestMetadata>(defaultConfig);
export const profileApi = new ProfileApiControllerApi<RequestMetadata>(defaultConfig);
export const customerApi = new CustomersApiControllerApi<RequestMetadata>(defaultConfig);
export const categoryApi = new CategoriesApiControllerApi<RequestMetadata>(defaultConfig);
export const statisticsApi = new StatisticsApiControllerApi<RequestMetadata>(defaultConfig);
export const invoicesApi = new InvoicesApiControllerApi<RequestMetadata>(defaultConfig);
export const expensesApi = new ExpensesApiControllerApi<RequestMetadata>(defaultConfig);
export const incomesApi = new IncomesApiControllerApi<RequestMetadata>(defaultConfig);
export const generalTaxApi = new GeneralTaxApiControllerApi<RequestMetadata>(defaultConfig);
export const documentsApi = new DocumentsApiControllerApi<RequestMetadata>(defaultConfig);
export const generalTaxesApi = new GeneralTaxApiControllerApi<RequestMetadata>(defaultConfig);
export const googleDriveStorageApi = new GoogleDriveStorageApiControllerApi<RequestMetadata>(defaultConfig);
export const oAuth2CallbackApi = new OAuth2CallbackControllerApi<RequestMetadata>(defaultConfig);
