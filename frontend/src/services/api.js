import axios from 'axios';
import qs from 'qs';
import jwtDecode from 'jwt-decode';
import {
  API_FATAL_ERROR_EVENT,
  LOADING_FINISHED_EVENT,
  LOADING_STARTED_EVENT,
  LOGIN_REQUIRED_EVENT,
} from '@/services/events';
import pageRequest from './api-filtering';

const { CancelToken } = axios;

const apiToken = {
  jwtToken: null,
  isAdmin: false,
  isTransient: false,
};

function updateApiToken(jwtToken) {
  apiToken.jwtToken = jwtToken;
  apiToken.isAdmin = false;
  apiToken.isTransient = false;
  if (jwtToken) {
    const decodedToken = jwtDecode(jwtToken);
    apiToken.isAdmin = decodedToken.roles && decodedToken.roles.indexOf('ADMIN') >= 0;
    apiToken.isTransient = decodedToken.transient;
  }
}

export const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer(params) {
    return qs.stringify(params, { arrayFormat: 'repeat' });
  },
});

let refreshTokenTimer;

function cancelTokenRefresh() {
  if (refreshTokenTimer) {
    clearTimeout(refreshTokenTimer);
  }
}

function scheduleTokenRefresh() {
  cancelTokenRefresh();

  const token = jwtDecode(apiToken.jwtToken);
  const timeout = token.exp * 1000 - Date.now() - 30000;
  refreshTokenTimer = setTimeout(refreshToken, timeout);
}

async function refreshToken() {
  if (await api.tryAutoLogin()) {
    scheduleTokenRefresh();
  } else {
    LOGIN_REQUIRED_EVENT.emit();
  }
}

api.createCancelToken = function createCancelToken() {
  return CancelToken.source();
};

api.login = async function login(request) {
  cancelTokenRefresh();
  const response = await api.post('/auth/login', request, {
    skipGlobalErrorHandler: true,
  });
  updateApiToken(response.data.token);
  scheduleTokenRefresh();
};

api.logout = async function logout(request) {
  cancelTokenRefresh();
  await api.post('/auth/logout', request);
  updateApiToken(null);
};

function applyAuthorization(config) {
  const { headers, ...otherConfig } = config;
  if (apiToken.jwtToken) {
    headers.Authorization = `Bearer ${apiToken.jwtToken}`;
  }
  return { headers, ...otherConfig };
}

function emitLoadingStartedEvent() {
  LOADING_STARTED_EVENT.emit();
}

function emitLoadingFinishedEvent() {
  LOADING_FINISHED_EVENT.emit();
}

api.interceptors.request.use(
  (config) => {
    emitLoadingStartedEvent();
    return applyAuthorization(config);
  },
  (error) => {
    emitLoadingFinishedEvent();
    return Promise.reject(error);
  },
);

api.tryAutoLogin = async function tryAutoLogin() {
  cancelTokenRefresh();

  try {
    const response = await api.post('/auth/token', {}, {
      withCredentials: true,
      skipGlobalErrorHandler: true,
    });

    updateApiToken(response.data.token);
    scheduleTokenRefresh();

    return true;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
};

api.loginBySharedToken = async function loginBySharedToken(sharedToken) {
  cancelTokenRefresh();

  try {
    const tokenLoginResponse = await api.post(`/auth/login?sharedWorkspaceToken=${sharedToken}`);

    updateApiToken(tokenLoginResponse.data.token);

    return true;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
};

api.interceptors.response.use(
  (response) => {
    emitLoadingFinishedEvent();
    return response;
  },
  async (error) => {
    if (error.response && error.response.status === 401
      && error.response.config.url !== '/auth/token'
      && error.response.config.url !== '/auth/login') {
      if (await api.tryAutoLogin()) {
        const config = applyAuthorization(error.config);
        config.baseURL = null;
        return axios.request(config);
      }
      LOGIN_REQUIRED_EVENT.emit();
    } else if (error.response && error.response.status >= 400 && !error.config.skipGlobalErrorHandler) {
      API_FATAL_ERROR_EVENT.emit(error);
    }
    emitLoadingFinishedEvent();
    return Promise.reject(error);
  },
);

api.isCancel = function isCancel(e) {
  return axios.isCancel(e);
};

api.pageRequest = pageRequest.bind(api);

api.dateToString = function dateToString(date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
};

api.isLoggedIn = function isLoggedIn() {
  return apiToken.jwtToken != null;
};

api.getToken = function getToken() {
  return apiToken.jwtToken;
};

api.isAdmin = function isAdmin() {
  return apiToken.isAdmin;
};

api.isCurrentUserTransient = function isCurrentUserTransient() {
  return apiToken.isTransient;
};

api.isCurrentUserRegular = function isCurrentUserRegular() {
  return !api.isCurrentUserTransient();
};

export default api;
