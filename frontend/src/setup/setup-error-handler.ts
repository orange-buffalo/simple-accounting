import { App } from 'vue';
import useNotifications from '@/components/notifications/use-notifications';
import { $t } from '@/services/i18n';

export default function setupErrorHandler(app: App) {
  const { showErrorNotification } = useNotifications();
  // eslint-disable-next-line no-param-reassign
  app.config.errorHandler = (err, vm, info) => {
    console.error('Failure during component processing', err, vm, info);
    showErrorNotification($t.value.errorHandler.fatalErrorMessage());
  };
}
