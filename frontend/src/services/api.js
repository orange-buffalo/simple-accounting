import axios from 'axios'
import apiStore from './api-store'

const _api = axios.create({
  baseURL: '/api/v1',
  timeout: 2000
})

_api.interceptors.request.use(
    config => {
      if (apiStore.state.jwtToken) {
        config.headers['Authorization'] = `Bearer: ${apiStore.state.jwtToken}`
      }
      return config;
    },
    error => Promise.reject(error)
)

_api.interceptors.response.use(
    response => Promise.resolve(response),
    error => {
      return Promise.reject(error)
    }
)

export default _api
export const api = _api
