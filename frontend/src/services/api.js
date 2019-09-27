import axios from 'axios'
import EventBus from 'eventbusjs'
import qs from 'qs'
import {assign} from 'lodash'
import jwtDecode from 'jwt-decode'

const CancelToken = axios.CancelToken

const _api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer: function (params) {
    return qs.stringify(params, {arrayFormat: 'repeat'})
  }
})

let $store

let refreshTokenTimer

let cancelTokenRefresh = function () {
  if (refreshTokenTimer) {
    clearTimeout(refreshTokenTimer)
  }
}

let scheduleTokenRefresh = function () {
  cancelTokenRefresh()

  let token = jwtDecode($store.state.api.jwtToken)
  let timeout = token.exp * 1000 - Date.now() - 30000
  refreshTokenTimer = setTimeout(refreshToken, timeout)
}

let refreshToken = async function () {
  if (await _api.tryAutoLogin()) {
    scheduleTokenRefresh()
  } else {
    EventBus.dispatch(LOGIN_REQUIRED_EVENT)
  }
}

_api.createCancelToken = function () {
  return CancelToken.source()
}

_api.login = function (request) {
  cancelTokenRefresh()
  return _api
      .post('/auth/login', request)
      .then(response => {
        $store.commit('api/updateJwtToken', response.data.token)
        scheduleTokenRefresh()
      })
}

_api.logout = async function (request) {
  cancelTokenRefresh()
  await _api.post('/auth/logout', request)
  $store.commit('api/updateJwtToken', null)
}

let applyAuthorization = function (config) {
  let jwtToken = $store.state.api.jwtToken
  if (jwtToken) {
    config.headers['Authorization'] = `Bearer ${jwtToken}`
  }
}

_api.interceptors.request.use(
    config => {
      applyAuthorization(config)
      return config;
    },
    error => Promise.reject(error)
)

_api.tryAutoLogin = async function () {
  cancelTokenRefresh()

  try {
    let response = await _api.post('/auth/token', {}, {
      withCredentials: true
    })

    $store.commit('api/updateJwtToken', response.data.token)
    scheduleTokenRefresh()

    return true

  } catch (error) {
    if (error.response && error.response.status === 401) {
      return false
    } else {
      throw error
    }
  }
}

_api.loginBySharedToken = async function (sharedToken) {
  cancelTokenRefresh()

  try {
    let tokenLoginResponse = await api.post(`/auth/login?sharedWorkspaceToken=${sharedToken}`)

    $store.commit('api/updateJwtToken', tokenLoginResponse.data.token)

    return true

  } catch (error) {
    if (error.response && error.response.status === 401) {
      return false
    } else {
      throw error
    }
  }
}

_api.interceptors.response.use(
    response => Promise.resolve(response),
    async error => {
      if (error.response && error.response.status === 401 && error.response.config.url !== '/api/auth/token') {
        if (await _api.tryAutoLogin()) {
          applyAuthorization(error.config)
          error.config.baseURL = null
          return axios.request(error.config)
        } else {
          EventBus.dispatch(LOGIN_REQUIRED_EVENT)
        }

      } else {
        return Promise.reject(error)
      }
    }
)

_api.isCancel = function (e) {
  return axios.isCancel(e)
}

_api.pageRequest = function (uri) {
  let limit = 10
  let page = 1
  let customConfig = {}
  let filters = {}

  let addFilter = (property, value, operator) => {
    if (typeof value !== 'undefined' && value !== null) {
      let filter = {}

      if (value instanceof Array) {
        filter[property] = value.map(item => {
          let itemFilter = {}
          itemFilter[operator] = item
          return itemFilter
        })
      } else {
        filter[property] = {}
        filter[property][operator] = value
      }

      assign(filters, filter)
    }
  }

  return {
    limit: function (value) {
      limit = value
      return this
    },

    eager: function () {
      // we still want to limit the data amount with some reasonable number
      return this.limit(100)
    },

    page: function (value) {
      page = value
      return this
    },

    config: function (value) {
      customConfig = value
      return this
    },

    eqFilter: function (property, value) {
      addFilter(property, value, 'eq')
      return this
    },

    get: function () {
      let params = {
        limit: limit,
        page: page
      }
      params = assign(params, filters)

      let config = {
        params: params
      }
      config = assign(config, customConfig)

      return _api.get(uri, config)
    },

    getPage: function () {
      return this.get().then(response => response.data)
    },

    getPageData: function () {
      return this.getPage().then(page => page.data)
    }
  }
}

_api.dateToString = function (date) {
  return date.getFullYear() + '-' +
      ('0' + (date.getMonth() + 1)).slice(-2) + '-' +
      ('0' + date.getDate()).slice(-2);
}

export default _api
export const api = _api
export const initApi = function (store) {
  $store = store
}
export const LOGIN_REQUIRED_EVENT = 'login-required'
