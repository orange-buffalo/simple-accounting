import { getAuthorizationHeader, tryAutoLogin } from '@/services/api/auth';
import type { FetchParams, Middleware } from '@/services/api/generated';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';

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

export const authorizationTokenInterceptor: Middleware = {
  async pre({ url, init }): Promise<FetchParams | void> {
    return {
      url,
      init: applyAuthorization(init),
    };
  },
};

export const expiredTokenInterceptor: Middleware = {
  async post({ response, fetch, init, url }): Promise<Response | void> {
    if (response.status === 401) {
      if (await tryAutoLogin()) {
        return fetch(url, applyAuthorization(init));
      }
      LOGIN_REQUIRED_EVENT.emit();
    }
  },
};
