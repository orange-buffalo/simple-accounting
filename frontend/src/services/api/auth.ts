import axios from 'axios';
import jwtDecode from 'jwt-decode';
import { AxiosRequestConfig } from 'openapi-client-axios';
import { Client as SimpleAccountingClient } from '@/services/api/api-client-definition';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';
import { LoginRequest } from '@/services/api/api-types';

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

let apiClient: SimpleAccountingClient;

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

let refreshTokenTimer: NodeJS.Timeout;

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

async function tryAutoLogin() {
  cancelTokenRefresh();

  try {
    const response = await apiClient.refreshToken(null, null, {
      withCredentials: true,
      skipGlobalErrorHandler: true,
    } as any);

    updateApiToken(response.data.token);
    scheduleTokenRefresh();

    return true;
  } catch (error: any) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
}

export function applyAuthorization(config: AxiosRequestConfig): AxiosRequestConfig {
  const {
    headers,
    ...otherConfig
  } = config;
  if (apiToken.jwtToken) {
    headers.Authorization = `Bearer ${apiToken.jwtToken}`;
  }
  return { headers, ...otherConfig };
}

export async function handleErrorResponse(error: any): Promise<any | null> {
  if (error.response && error.response.status === 401
    && error.response.config.url !== '/api/auth/token'
    && error.response.config.url !== '/auth/token'
    && error.response.config.url !== '/api/auth/login'
    && error.response.config.url !== '/auth/login') {
    if (await tryAutoLogin()) {
      const config = applyAuthorization(error.config);
      config.baseURL = undefined;
      return axios.request(config);
    }
    LOGIN_REQUIRED_EVENT.emit();
  }
  return null;
}

async function login(request: LoginRequest) {
  cancelTokenRefresh();
  const response = await apiClient.login(null, request, {
    skipGlobalErrorHandler: true,
  } as any);
  updateApiToken(response.data.token);
  scheduleTokenRefresh();
}

async function logout() {
  cancelTokenRefresh();
  await apiClient.logout();
  updateApiToken(null);
}

async function loginBySharedToken(sharedToken: string) {
  cancelTokenRefresh();

  try {
    const tokenLoginResponse = await apiClient.loginBySharedWorkspaceToken({
      sharedWorkspaceToken: sharedToken,
    });

    updateApiToken(tokenLoginResponse.data.token);

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

export function initAuth(client: SimpleAccountingClient) {
  apiClient = client;
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
