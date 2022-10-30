import { ElMessage } from 'element-plus';
import { h } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import '@/components/notifications/notifications.scss';

interface NotificationType {
  icon: string,
  notificationClass: string,
}

const ERROR_NOTIFICATION: NotificationType = {
  icon: 'error',
  notificationClass: 'error',
};

function showNotification(
  message: string,
  type: NotificationType,
) {
  ElMessage({
    showClose: true,
    message,
    duration: 0,
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

export default function useNotifications() {
  return {
    showErrorNotification,
  };
}
