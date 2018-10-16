import axios from 'axios'
import EventBus from 'eventbusjs'

const CancelToken = axios.CancelToken;

const _api = axios.create({
  baseURL: '/api/v1',
  timeout: 2000
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

export default _api
export const api = _api
export const initApi = function (store) {
  $store = store
}
export const LOGIN_REQUIRED_EVENT = 'login-required'
export const SUCCESSFUL_LOGIN_EVENT = 'successful-login'
