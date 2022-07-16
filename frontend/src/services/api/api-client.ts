import qs from 'qs';
import {
  AuthenticationApiControllerApi,
  Configuration, ProfileApiControllerApi, WorkspacesApiControllerApi,
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
