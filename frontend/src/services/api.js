import axios from 'axios'
import EventBus from 'eventbusjs'
import qs from 'qs'
import merge from 'merge'

const CancelToken = axios.CancelToken;

const _api = axios.create({
  baseURL: '/api/v1',
  timeout: 2000,
  paramsSerializer: function (params) {
    return qs.stringify(params, {arrayFormat: 'repeat'})
  }
})

let $store

_api.createCancelToken = function () {
  return CancelToken.source()
}

_api.login = function (request) {
  return new Promise((resolve, reject) => {
    _api
        .post('/auth/login', request)
        .then(response => {
          $store.commit('api/updateJwtToken', response.data.token)
          EventBus.dispatch(SUCCESSFUL_LOGIN_EVENT)
          resolve()
        })
        .catch(() => {
          reject()
        })
  })
}

_api.interceptors.request.use(
    config => {
      let jwtToken = $store.state.api.jwtToken
      if (jwtToken) {
        config.headers['Authorization'] = `Bearer ${jwtToken}`
      }
      return config;
    },
    error => Promise.reject(error)
)

_api.interceptors.response.use(
    response => Promise.resolve(response),
    error => {
      if (error.response && error.response.status === 401) {
        EventBus.dispatch(LOGIN_REQUIRED_EVENT)
      }
      return Promise.reject(error)
    }
)

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

      merge(filters, filter)
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
      params = merge(params, filters)

      let config = {
        params: params
      }
      config = merge(config, customConfig)

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

export default _api
export const api = _api
export const initApi = function (store) {
  $store = store
}
export const LOGIN_REQUIRED_EVENT = 'login-required'
export const SUCCESSFUL_LOGIN_EVENT = 'successful-login'
