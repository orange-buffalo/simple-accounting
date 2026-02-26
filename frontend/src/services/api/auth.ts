import { jwtDecode } from 'jwt-decode';
import { LOGIN_REQUIRED_EVENT } from '@/services/events';
import { graphql } from '@/services/api/gql';
import { executeRawGqlMutation } from '@/services/api/gql-raw-client';

const refreshTokenMutation = graphql(/* GraphQL */ `
    mutation refreshAccessToken {
        refreshAccessToken {
            accessToken
        }
    }
`);

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
    const data = await executeRawGqlMutation(refreshTokenMutation, {});
    const accessToken = data.refreshAccessToken.accessToken;
    if (!accessToken) {
      return false;
    }

    updateApiToken(accessToken);
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

const loginMutation = graphql(/* GraphQL */ `
  mutation createAccessTokenByCredentials(
    $userName: String!
    $password: String!
    $issueRefreshTokenCookie: Boolean
  ) {
    createAccessTokenByCredentials(
      userName: $userName
      password: $password
      issueRefreshTokenCookie: $issueRefreshTokenCookie
    ) {
      accessToken
    }
  }
`);

interface LoginRequest {
  userName: string;
  password: string;
  rememberMe: boolean;
}

async function login(loginRequest: LoginRequest) {
  cancelTokenRefresh();
  const data = await executeRawGqlMutation(loginMutation, {
    userName: loginRequest.userName,
    password: loginRequest.password,
    issueRefreshTokenCookie: loginRequest.rememberMe,
  });
  updateApiToken(data.createAccessTokenByCredentials.accessToken);
  scheduleTokenRefresh();
}

async function logout() {
  cancelTokenRefresh();
  try {
    await fetch('/api/graphql', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(
        { query: 'mutation { invalidateRefreshToken }' },
      ),
    });
  } finally {
    updateApiToken(null);
  }
}

const loginBySharedTokenMutation = graphql(/* GraphQL */ `
  mutation createAccessTokenByWorkspaceAccessToken(
    $workspaceAccessToken: String!
  ) {
    createAccessTokenByWorkspaceAccessToken(
      workspaceAccessToken: $workspaceAccessToken
    ) {
      accessToken
    }
  }
`);

async function loginBySharedToken(sharedToken: string) {
  cancelTokenRefresh();

  try {
    const data = await executeRawGqlMutation(loginBySharedTokenMutation, {
      workspaceAccessToken: sharedToken,
    });

    updateApiToken(data.createAccessTokenByWorkspaceAccessToken.accessToken);

    return true;
  } catch (error: any) {
    if (error.extensions?.errorCode === 'INVALID_WORKSPACE_ACCESS_TOKEN') {
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

  getToken(): (string | null);

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
