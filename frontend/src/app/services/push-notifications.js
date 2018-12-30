import {isNil} from 'lodash'

import 'eventsource/lib/eventsource-polyfill'
import {api} from '@/services/api'

let $store

export const PushNotifications = {

  _eventSource: null,
  _eventListeners: [],

  _init: function () {
    let jwtToken = $store.state.api.jwtToken

    this._eventSource = new EventSourcePolyfill(
        "/api/v1/user/push-notifications", {
          headers: {
            'Authorization': `Bearer ${jwtToken}`
          }
        })

    this._eventSource.onmessage = event => {

      let message = JSON.parse(event.data)
      this._eventListeners
          .filter(it => it.eventName === message.eventName)
          .forEach(it => it.callback(message.data))
    };

    this._eventSource.onerror = event => {
      if (event.status && event.status === 401) {
        this._eventSource.close()
        api.tryAutoLogin().then(() => {
          jwtToken = $store.state.api.jwtToken
          this._init()
        })
      } else if (this._eventSource.readyState === 2) {
        this._eventSource = null
      }
    };
  },

  subscribe: function (eventName, callback) {
    if (isNil(this._eventSource)) {
      this._init()
    }
    this._eventListeners.push({
      eventName: eventName,
      callback: callback
    })
  },

  unsubscribe: function (eventName, callback) {
    this._eventListeners = this._eventListeners
        .filter(it => it.eventName !== eventName && it.callback !== callback)
  }
}

export const initPushNotifications = function (store) {
  $store = store
}
