import 'eventsource/lib/eventsource-polyfill';
import { api } from '@/services/api';

let $store;

export const pushNotifications = {

  eventSource: null,
  eventListeners: [],

  init() {
    const { jwtToken } = $store.state.api;

    // eslint-disable-next-line no-undef
    this.eventSource = new EventSourcePolyfill(
      '/api/push-notifications', {
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      },
    );

    this.eventSource.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.eventListeners
        .filter(it => it.eventName === message.eventName)
        .forEach(it => it.callback(message.data));
    };

    this.eventSource.onerror = async (event) => {
      if (event.status && event.status === 401) {
        this.eventSource.close();
        if (await api.tryAutoLogin()) {
          this.init();
        }
      } else if (this.eventSource.readyState === 2) {
        this.eventSource = null;
      }
    };
  },

  subscribe(eventName, callback) {
    if (this.eventSource == null) {
      this.init();
    }
    this.eventListeners.push({
      eventName,
      callback,
    });
  },

  unsubscribe(eventName, callback) {
    this.eventListeners = this.eventListeners
      .filter(it => it.eventName !== eventName && it.callback !== callback);

    if (this.eventListeners.length === 0) {
      this.eventSource.close();
      this.eventSource = null;
    }
  },
};

export const initPushNotifications = function initPushNotifications(store) {
  $store = store;
};
