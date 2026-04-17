import { useAuth } from '@/services/api';

export type PushNotificationListenerCallback<T = unknown> = (data: T) => void;

interface PushNotificationListener<T = undefined> {
  readonly eventName: string,
  readonly callback: PushNotificationListenerCallback<T>,
}

let ws: WebSocket | undefined;
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
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/api/graphql/subscriptions`;

  ws = new WebSocket(wsUrl, 'graphql-transport-ws');

  ws.addEventListener('open', () => {
    ws?.send(JSON.stringify({
      type: 'connection_init',
      payload: { token: getToken() },
    }));
  });

  ws.addEventListener('message', (event) => {
    const message = JSON.parse(event.data);
    if (message.type === 'connection_ack') {
      ws?.send(JSON.stringify({
        id: '1',
        type: 'subscribe',
        payload: {
          query: 'subscription { pushNotifications { eventName data } }',
        },
      }));
    } else if (message.type === 'next') {
      const notification = message.payload?.data?.pushNotifications;
      if (notification) {
        const data = notification.data != null ? JSON.parse(notification.data) : undefined;
        notifyListeners(notification.eventName, data);
      }
    }
  });

  ws.addEventListener('close', async () => {
    ws = undefined;
    if (eventListeners.length > 0) {
      if (await tryAutoLogin()) {
        init();
      }
    }
  });

  ws.addEventListener('error', () => {
    ws?.close();
  });
}

export function subscribeToPushNotifications<T>(eventName: string, callback: PushNotificationListenerCallback<T>) {
  if (!ws) {
    init();
  }
  eventListeners.push({
    eventName,
    callback: callback as PushNotificationListenerCallback,
  });
}

export function unsubscribeFromPushNotifications<T>(eventName: string, callback: PushNotificationListenerCallback<T>) {
  eventListeners = eventListeners
    .filter((it) => it.eventName !== eventName || it.callback !== callback);

  if (eventListeners.length === 0 && ws) {
    ws.close();
    ws = undefined;
  }
}

export function pushEvent(eventName: string, data: unknown) {
  notifyListeners(eventName, data);
}
