import EventSource from 'eventsource';
import type { CurrentUserPushNotificationMessage } from '@/services/api';
import { useAuth } from '@/services/api';

export type PushNotificationListenerCallback<T = unknown> = (data: T) => void;

interface PushNotificationListener<T = undefined> {
  readonly eventName: string,
  readonly callback: PushNotificationListenerCallback<T>,
}

let eventSource: EventSource | undefined;
let eventListeners: Array<PushNotificationListener<unknown>> = [];

function notifyListeners(eventName: string, data: unknown) {
  eventListeners
    .filter((it) => it.eventName === eventName)
    .forEach((it) => it.callback(data));
}

const {
  getToken,
  tryAutoLogin,
} = useAuth();

function init() {
  // eslint-disable-next-line no-undef
  eventSource = new EventSource('/api/push-notifications', {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });

  eventSource.onmessage = (event) => {
    const message: CurrentUserPushNotificationMessage = JSON.parse(event.data);
    notifyListeners(message.eventName, message.data);
  };

  // TODO remove any when https://github.com/DefinitelyTyped/DefinitelyTyped/discussions/62401 is resolved
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  eventSource.onerror = async (event: any) => {
    if (event.status && event.status === 401 && eventSource) {
      eventSource.close();
      if (await tryAutoLogin()) {
        init();
      }
    } else if (eventSource && eventSource.readyState === EventSource.ReadyState.CLOSED) {
      eventSource = undefined;
    }
  };
}

export function subscribeToPushNotifications(eventName: string, callback: PushNotificationListenerCallback) {
  if (!eventSource) {
    init();
  }
  eventListeners.push({
    eventName,
    callback,
  });
}

export function unsubscribeFromPushNotifications(eventName: string, callback: PushNotificationListenerCallback) {
  eventListeners = eventListeners
    .filter((it) => it.eventName !== eventName && it.callback !== callback);

  if (eventListeners.length === 0 && eventSource) {
    eventSource.close();
    eventSource = undefined;
  }
}

export function pushEvent(eventName: string, data: unknown) {
  notifyListeners(eventName, data);
}
