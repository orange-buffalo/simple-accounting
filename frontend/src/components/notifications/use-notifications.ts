import { ElMessage } from 'element-plus';
import { h } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import '@/components/notifications/notifications.scss';

export interface NotificationOptions {
  duration: number;
}

export const NOTIFICATION_ALWAYS_VISIBLE_DURATION = 0;

interface NotificationType extends NotificationOptions {
  icon: string;
  notificationClass: string;
  elPlusType: 'success' | 'warning' | 'info' | 'error';
}

const ERROR_NOTIFICATION: NotificationType = {
  icon: 'error',
  notificationClass: 'error',
  duration: 0,
  elPlusType: 'error',
};

const SUCCESS_NOTIFICATION: NotificationType = {
  icon: 'success',
  notificationClass: 'success',
  duration: 5000,
  elPlusType: 'success',
};

const WARNING_NOTIFICATION: NotificationType = {
  icon: 'warning-circle',
  notificationClass: 'warning',
  duration: 10000,
  elPlusType: 'warning',
};

function showNotification(message: string, type: NotificationType) {
  // @ts-expect-error
  ElMessage({
    showClose: true,
    message,
    duration: type.duration,
    type: type.elPlusType,
    icon: h(SaIcon, {
      icon: type.icon,
      size: 18,
    }),
    customClass: `sa-notification sa-notification--${type.notificationClass}`,
  });
}

function showErrorNotification(message: string) {
  showNotification(message, ERROR_NOTIFICATION);
}

function showSuccessNotification(message: string) {
  showNotification(message, SUCCESS_NOTIFICATION);
}

function showWarningNotification(message: string, options?: NotificationOptions) {
  showNotification(message, {
    ...WARNING_NOTIFICATION,
    ...options,
  });
}

export default function useNotifications() {
  return {
    showErrorNotification,
    showSuccessNotification,
    showWarningNotification,
  };
}
