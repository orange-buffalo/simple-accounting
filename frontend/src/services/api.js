import axios from 'axios'
import EventBus from 'eventbusjs'

const _api = axios.create({
  baseURL: '/api/v1',
  timeout: 2000
})

let $store

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
