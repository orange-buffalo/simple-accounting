import { EventSourcePolyfill } from 'event-source-polyfill';
import type { CurrentUserPushNotificationMessage } from '@/services/api';
import { useAuth } from '@/services/api';

export type PushNotificationListenerCallback<T = unknown> = (data: T) => void;

interface PushNotificationListener<T = undefined> {
  readonly eventName: string;
  readonly callback: PushNotificationListenerCallback<T>;
}

let eventSource: EventSourcePolyfill | undefined;
let eventListeners: Array<PushNotificationListener<unknown>> = [];

function notifyListeners(eventName: string, data: unknown) {
  eventListeners.filter((it) => it.eventName === eventName).forEach((it) => it.callback(data));
}

const { getToken, tryAutoLogin } = useAuth();

function init() {
  eventSource = new EventSourcePolyfill('/api/push-notifications', {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });

  eventSource.addEventListener('message', (event) => {
    const message: CurrentUserPushNotificationMessage = JSON.parse(event.data);
    notifyListeners(message.eventName, message.data);
  });

  eventSource.addEventListener('error', async (event: any) => {
    if (event.status && event.status === 401 && eventSource) {
      eventSource.close();
      if (await tryAutoLogin()) {
        init();
      }
    } else if (eventSource && eventSource.readyState === EventSource.CLOSED) {
      eventSource = undefined;
    }
  });
}

export function subscribeToPushNotifications<T>(eventName: string, callback: PushNotificationListenerCallback<T>) {
  if (!eventSource) {
    init();
  }
  eventListeners.push({
    eventName,
    callback: callback as PushNotificationListenerCallback,
  });
}

export function unsubscribeFromPushNotifications<T>(eventName: string, callback: PushNotificationListenerCallback<T>) {
  eventListeners = eventListeners.filter((it) => it.eventName !== eventName && it.callback !== callback);

  if (eventListeners.length === 0 && eventSource) {
    eventSource.close();
    eventSource = undefined;
  }
}

export function pushEvent(eventName: string, data: unknown) {
  notifyListeners(eventName, data);
}
