import { ElMessage } from 'element-plus';
import { h } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import '@/components/notifications/notifications.scss';

interface NotificationType {
  icon: string,
  notificationClass: string,
  duration: number,
}

const ERROR_NOTIFICATION: NotificationType = {
  icon: 'error',
  notificationClass: 'error',
  duration: 0,
};

const SUCCESS_NOTIFICATION: NotificationType = {
  icon: 'success',
  notificationClass: 'success',
  duration: 5000,
};

const WARNING_NOTIFICATION: NotificationType = {
  icon: 'warning-circle',
  notificationClass: 'warning',
  duration: 10000,
};

function showNotification(
  message: string,
  type: NotificationType,
) {
  ElMessage({
    showClose: true,
    message,
    duration: type.duration,
    // @ts-ignore
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

function showWarningNotification(message: string) {
  showNotification(message, WARNING_NOTIFICATION);
}

export default function useNotifications() {
  return {
    showErrorNotification,
    showSuccessNotification,
    showWarningNotification,
  };
}
