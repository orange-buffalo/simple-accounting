import type { FetchParams, Middleware } from '@/services/api/generated';
import type { RequestMetadata } from '@/services/api/api-client';
import { getAuthorizationHeader, tryAutoLogin } from '@/services/api/auth';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';

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

export const authorizationTokenInterceptor: Middleware<RequestMetadata> = {
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

export const expiredTokenInterceptor: Middleware<RequestMetadata> = {
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
