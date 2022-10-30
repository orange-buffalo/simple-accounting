import jwtDecode from 'jwt-decode';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';
import type { LoginRequest } from '@/services/api/api-types';
import { authApi } from '@/services/api/api-client';

interface ApiToken {
  jwtToken: string | null,
  isAdmin: boolean,
  isTransient: boolean,
}

interface JwtToken {
  roles: string[],
  transient: boolean,
  exp: number,
}

const apiToken: ApiToken = {
  jwtToken: null,
  isAdmin: false,
  isTransient: false,
};

function updateApiToken(jwtToken: string | null) {
  apiToken.jwtToken = jwtToken;
  apiToken.isAdmin = false;
  apiToken.isTransient = false;
  if (jwtToken) {
    const decodedToken = jwtDecode<JwtToken>(jwtToken);
    apiToken.isAdmin = decodedToken.roles && decodedToken.roles.indexOf('ADMIN') >= 0;
    apiToken.isTransient = decodedToken.transient;
  }
}

let refreshTokenTimer: number;

function cancelTokenRefresh() {
  if (refreshTokenTimer) {
    clearTimeout(refreshTokenTimer);
  }
}

function scheduleTokenRefresh() {
  cancelTokenRefresh();

  if (!apiToken.jwtToken) {
    throw new Error('Token expected to be defined');
  }
  const token = jwtDecode(apiToken.jwtToken) as JwtToken;
  const timeout = token.exp * 1000 - Date.now() - 30000;
  refreshTokenTimer = setTimeout(refreshToken, timeout);
}

async function refreshToken() {
  if (await tryAutoLogin()) {
    scheduleTokenRefresh();
  } else {
    LOGIN_REQUIRED_EVENT.emit();
  }
}

export async function tryAutoLogin() {
  cancelTokenRefresh();

  try {
    const response = await authApi.refreshToken({}, {
      credentials: 'include',
      // TODO
      // skipGlobalErrorHandler: true,
    });

    updateApiToken(response.token);
    scheduleTokenRefresh();

    return true;
    // eslint-disable-next-line
  } catch (error: any) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
}

export function getAuthorizationHeader(): string | null {
  if (apiToken.jwtToken) {
    return `Bearer ${apiToken.jwtToken}`;
  }
  return null;
}

async function login(request: LoginRequest) {
  cancelTokenRefresh();
  const response = await authApi.login({ loginRequest: request }, {
    // TODO
    // skipGlobalErrorHandler: true,
  });
  updateApiToken(response.token);
  scheduleTokenRefresh();
}

async function logout() {
  cancelTokenRefresh();
  await authApi.logout();
  updateApiToken(null);
}

async function loginBySharedToken(sharedToken: string) {
  cancelTokenRefresh();

  try {
    const tokenLoginResponse = await authApi.loginBySharedWorkspaceToken({
      sharedWorkspaceToken: sharedToken,
    });

    updateApiToken(tokenLoginResponse.token);

    return true;
    // eslint-disable-next-line
  } catch (error: any) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
}

function isCurrentUserTransient() {
  return apiToken.isTransient;
}

export function useAuth() {
  return {
    getToken(): string | null {
      return apiToken.jwtToken;
    },
    tryAutoLogin,
    login,
    logout,
    loginBySharedToken,
    isLoggedIn() {
      return apiToken.jwtToken != null;
    },
    isAdmin() {
      return apiToken.isAdmin;
    },
    isCurrentUserTransient,
    isCurrentUserRegular() {
      return !isCurrentUserTransient();
    },
  };
}
