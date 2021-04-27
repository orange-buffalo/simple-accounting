import axios from 'axios';
import qs from 'qs';
import {
  API_FATAL_ERROR_EVENT,
  LOADING_FINISHED_EVENT,
  LOADING_STARTED_EVENT,
} from '@/services/events';
import { applyAuthorization, handleErrorResponse, useAuth } from '@/services/api/auth';
import pageRequest from './api-filtering';

const { CancelToken } = axios;

/**
 * @deprecated use api-client
 */
export const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer(params) {
    return qs.stringify(params, { arrayFormat: 'repeat' });
  },
});

api.createCancelToken = function createCancelToken() {
  return CancelToken.source();
};

api.login = async function login(request) {
  const { login: authLogin } = useAuth();
  await authLogin(request);
};

api.logout = async function logout(request) {
  const { logout: authLogout } = useAuth();
  await authLogout(request);
};

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
  const { tryAutoLogin: authAutoLogin } = useAuth();
  await authAutoLogin();
};

api.loginBySharedToken = async function loginBySharedToken(sharedToken) {
  const { loginBySharedToken: authLoginBySharedToken } = useAuth();
  await authLoginBySharedToken(sharedToken);
};

api.interceptors.response.use(
  (response) => {
    emitLoadingFinishedEvent();
    return response;
  },
  async (error) => {
    const errorHandlingResponse = await handleErrorResponse(error);
    if (errorHandlingResponse) {
      return errorHandlingResponse;
    }
    if (error.response && error.response.status !== 401
      && error.response.status >= 400
      && !error.config.skipGlobalErrorHandler) {
      API_FATAL_ERROR_EVENT.emit(error);
    }
    emitLoadingFinishedEvent();
    throw error;
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
  return useAuth()
    .isLoggedIn();
};

api.getToken = function getToken() {
  return useAuth()
    .getToken();
};

api.isAdmin = function isAdmin() {
  return useAuth()
    .isAdmin();
};

api.isCurrentUserTransient = function isCurrentUserTransient() {
  return useAuth()
    .isCurrentUserTransient();
};

api.isCurrentUserRegular = function isCurrentUserRegular() {
  return useAuth()
    .isCurrentUserRegular();
};

export default api;
