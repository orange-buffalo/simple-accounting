import axios from 'axios';
import EventBus from 'eventbusjs';
import qs from 'qs';
import jwtDecode from 'jwt-decode';

const { CancelToken } = axios;

export const LOGIN_REQUIRED_EVENT = 'login-required';

export const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer(params) {
    return qs.stringify(params, { arrayFormat: 'repeat' });
  },
});

let $store;

let refreshTokenTimer;

function cancelTokenRefresh() {
  if (refreshTokenTimer) {
    clearTimeout(refreshTokenTimer);
  }
}

function scheduleTokenRefresh() {
  cancelTokenRefresh();

  const token = jwtDecode($store.state.api.jwtToken);
  const timeout = token.exp * 1000 - Date.now() - 30000;
  refreshTokenTimer = setTimeout(refreshToken, timeout);
}

async function refreshToken() {
  if (await api.tryAutoLogin()) {
    scheduleTokenRefresh();
  } else {
    EventBus.dispatch(LOGIN_REQUIRED_EVENT);
  }
}

api.createCancelToken = function createCancelToken() {
  return CancelToken.source();
};

api.login = async function login(request) {
  cancelTokenRefresh();
  const response = await api.post('/auth/login', request);
  $store.commit('api/updateJwtToken', response.data.token);
  scheduleTokenRefresh();
};

api.logout = async function logout(request) {
  cancelTokenRefresh();
  await api.post('/auth/logout', request);
  $store.commit('api/updateJwtToken', null);
};

function applyAuthorization(config) {
  const { jwtToken } = $store.state.api;
  const { headers, ...otherConfig } = config;
  if (jwtToken) {
    headers.Authorization = `Bearer ${jwtToken}`;
  }
  return { headers, ...otherConfig };
}

api.interceptors.request.use(
  config => applyAuthorization(config),
  error => Promise.reject(error),
);

api.tryAutoLogin = async function tryAutoLogin() {
  cancelTokenRefresh();

  try {
    const response = await api.post('/auth/token', {}, {
      withCredentials: true,
    });

    $store.commit('api/updateJwtToken', response.data.token);
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

    $store.commit('api/updateJwtToken', tokenLoginResponse.data.token);

    return true;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      return false;
    }
    throw error;
  }
};

api.interceptors.response.use(
  response => response,
  async (error) => {
    if (error.response && error.response.status === 401
      && error.response.config.url !== '/api/auth/token'
      && error.response.config.url !== '/api/auth/login') {
      if (await api.tryAutoLogin()) {
        const config = applyAuthorization(error.config);
        config.baseURL = null;
        return axios.request(config);
      }
      EventBus.dispatch(LOGIN_REQUIRED_EVENT);
    }
    return Promise.reject(error);
  },
);

api.isCancel = function isCancel(e) {
  return axios.isCancel(e);
};

api.pageRequest = function pageRequest(uri) {
  let limit = 10;
  let page = 1;
  let customConfig = {};
  let filters = {};

  const addFilter = (property, value, operator) => {
    if (value != null) {
      const filter = {};

      if (value instanceof Array) {
        filter[property] = value.map((item) => {
          const itemFilter = {};
          itemFilter[operator] = item;
          return itemFilter;
        });
      } else {
        filter[property] = {};
        filter[property][operator] = value;
      }

      filters = { ...filters, ...filter };
    }
  };

  return {
    limit(value) {
      limit = value;
      return this;
    },

    eager() {
      // we still want to limit the data amount with some reasonable number
      return this.limit(100);
    },

    page(value) {
      page = value;
      return this;
    },

    config(value) {
      customConfig = value;
      return this;
    },

    eqFilter(property, value) {
      addFilter(property, value, 'eq');
      return this;
    },

    get() {
      const params = {
        limit,
        page,
        ...filters,
      };
      const config = { params, ...customConfig };
      return api.get(uri, config);
    },

    getPage() {
      return this.get()
        .then(response => response.data);
    },

    getPageData() {
      return this.getPage()
        .then(pageResponse => pageResponse.data);
    },
  };
};

api.dateToString = function dateToString(date) {
  return `${date.getFullYear()}-${
    (`0${date.getMonth() + 1}`).slice(-2)}-${
    (`0${date.getDate()}`).slice(-2)}`;
};

export default api;
export const initApi = function initApi(store) {
  $store = store;
};
