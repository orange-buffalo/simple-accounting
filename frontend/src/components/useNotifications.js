import { Message } from 'element-ui';
import { h } from '@vue/composition-api';
import SaIcon from '@/components/SaIcon';

const NotificationType = {
  ERROR: {
    icon: 'error',
    notificationClass: 'error',
  },
};

function showNotification({
  message,
  type,
}) {
  const content = h(
    'span', {
      class: {
        'sa-notification__content': true,
      },
    },
    [
      h(SaIcon, {
        props: {
          icon: type.icon,
        },
        class: {
          'sa-notification__content__icon': true,
        },
      }),
      h('span', {
        class: {
          'sa-notification__content__text': true,
        },
      },
      message),
    ],
  );

  Message({
    showClose: true,
    message: content,
    duration: 0,
    iconClass: 'sa-notification__icon',
    customClass: `sa-notification--${type.notificationClass}`,
  });
}

function showErrorNotification(options) {
  showNotification({
    ...options,
    type: NotificationType.ERROR,
  });
}

export default function useNotifications() {
  return {
    showErrorNotification,
  };
}
