import { jwtDecode } from 'jwt-decode';
import { authApi } from '@/services/api/api-client';
import type { LoginRequest } from '@/services/api/generated';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';

interface ApiToken {
  jwtToken: string | null;
  isAdmin: boolean;
  isTransient: boolean;
}

interface JwtToken {
  roles: string[];
  transient: boolean;
  exp: number;
}

const apiToken: ApiToken = {
  jwtToken: null,
  isAdmin: false,
  isTransient: false,
};

// TODO remove export and make private one migrated fully to GraphQL
export function updateApiToken(jwtToken: string | null) {
  apiToken.jwtToken = jwtToken;
  apiToken.isAdmin = false;
  apiToken.isTransient = false;
  if (jwtToken) {
    const decodedToken = jwtDecode<JwtToken>(jwtToken);
    apiToken.isAdmin = decodedToken.roles && decodedToken.roles.indexOf('ADMIN') >= 0;
    apiToken.isTransient = decodedToken.transient;
  }
}

let refreshTokenTimer: ReturnType<typeof setTimeout>;

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
    const response = await authApi.refreshToken(
      {},
      {
        credentials: 'include',
      },
    );

    updateApiToken(response.token);
    scheduleTokenRefresh();

    return true;
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

async function login(loginRequest: LoginRequest) {
  cancelTokenRefresh();
  const response = await authApi.login({ loginRequest });
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

export interface Auth {
  logout: () => Promise<void>;

  getToken(): string | null;

  isCurrentUserTransient: () => boolean;

  tryAutoLogin: () => Promise<boolean>;

  isLoggedIn(): boolean;

  isAdmin(): boolean;

  login: (request: LoginRequest) => Promise<void>;

  isCurrentUserRegular(): boolean;

  loginBySharedToken: (sharedToken: string) => Promise<boolean>;
}

export function useAuth(): Auth {
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
