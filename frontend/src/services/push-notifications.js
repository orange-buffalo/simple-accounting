import 'eventsource/lib/eventsource-polyfill';
import { api } from '@/services/api-legacy';

let eventSource = null;
let eventListeners = [];

function notifyListeners(eventName, data) {
  eventListeners
    .filter((it) => it.eventName === eventName)
    .forEach((it) => it.callback(data));
}

export default {

  init() {
    // eslint-disable-next-line no-undef
    eventSource = new EventSourcePolyfill(
      '/api/push-notifications', {
        headers: {
          Authorization: `Bearer ${api.getToken()}`,
        },
      },
    );

    eventSource.onmessage = (event) => {
      const message = JSON.parse(event.data);
      notifyListeners(message.eventName, message.data);
    };

    eventSource.onerror = async (event) => {
      if (event.status && event.status === 401) {
        eventSource.close();
        if (await api.tryAutoLogin()) {
          this.init();
        }
      } else if (eventSource.readyState === 2) {
        eventSource = null;
      }
    };
  },

  subscribe(eventName, callback) {
    if (eventSource == null) {
      this.init();
    }
    eventListeners.push({
      eventName,
      callback,
    });
  },

  unsubscribe(eventName, callback) {
    eventListeners = eventListeners
      .filter((it) => it.eventName !== eventName && it.callback !== callback);

    if (eventListeners.length === 0) {
      eventSource.close();
      eventSource = null;
    }
  },

  pushEvent(eventName, data) {
    notifyListeners(eventName, data);
  },
};
