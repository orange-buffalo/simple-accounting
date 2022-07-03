import qs from 'qs';
import {
  getAuthorizationHeader, tryAutoLogin,
} from '@/services/api/auth';
import {
  API_FATAL_ERROR_EVENT,
  LOADING_FINISHED_EVENT,
  LOADING_STARTED_EVENT, LOGIN_REQUIRED_EVENT,
} from '@/services/events';
import {
  AuthenticationApiControllerApi,
  Configuration, FetchError, ProfileApiControllerApi, ResponseError, WorkspacesApiControllerApi,
} from '@/services/api/generated';
import type {
  ConfigurationParameters,
  FetchParams,
  Middleware,
} from '@/services/api/generated';

// eslint-disable-next-line max-len
// todo: timeout of 10sec, https://thewebdev.info/2022/04/21/how-to-set-request-timeout-with-fetch-api/#:~:text=To%20set%20request%20timeout%20with%20Fetch%20API%2C%20we%20can%20use,url%2C%20%7B%20signal%3A%20controller.

function emitLoadingFinishedEvent() {
  LOADING_FINISHED_EVENT.emit();
}

const loadingEventsInterceptor: Middleware = {
  async pre() {
    LOADING_STARTED_EVENT.emit();
  },
  async post() {
    emitLoadingFinishedEvent();
  },
};

// eslint-disable-next-line no-undef
export function applyAuthorization(init: RequestInit): RequestInit {
  const authHeader = getAuthorizationHeader();
  if (!authHeader) return init;
  const headers = new Headers(init.headers);
  headers.append('Authorization', authHeader);
  return {
    ...init,
    headers,
  };
}

const authorizationTokenInterceptor: Middleware = {
  async pre({
    url,
    init,
  }): Promise<FetchParams | void> {
    return {
      url,
      init: applyAuthorization(init),
    };
  },
};

interface InterceptorMetadata {
  skipGlobalErrorHandler?: boolean;
}

const errorHandlingInterceptor: Middleware = {
  async post({
    response,
    metadata,
  }): Promise<Response | void> {
    if (response.status >= 400
      && (!metadata || !(metadata as InterceptorMetadata).skipGlobalErrorHandler)
    ) {
      API_FATAL_ERROR_EVENT.emit(new ResponseError(response));
    }
  },

  async onError({
    error,
    response,
    metadata,
  }): Promise<Response | void> {
    if (!response
      && (!metadata || !(metadata as InterceptorMetadata).skipGlobalErrorHandler)
    ) {
      API_FATAL_ERROR_EVENT.emit(new FetchError(error));
    }
  },
};

const expiredTokenInterceptor: Middleware = {
  async post({
    response,
    fetch,
    init,
    url,
  }): Promise<Response | void> {
    if (response.status === 401) {
      if (await tryAutoLogin()) {
        return fetch(url, applyAuthorization(init));
      }
      LOGIN_REQUIRED_EVENT.emit();
    }
  },
};

const defaultConfigParameters: ConfigurationParameters = {
  basePath: '',
  queryParamsStringify: (params) => qs.stringify(params, { arrayFormat: 'repeat' }),
  middleware: [
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
    loadingEventsInterceptor,
    authorizationTokenInterceptor,
    errorHandlingInterceptor,
  ],
}));

export const workspacesApi = new WorkspacesApiControllerApi(defaultConfig);
export const profileApi = new ProfileApiControllerApi(defaultConfig);
