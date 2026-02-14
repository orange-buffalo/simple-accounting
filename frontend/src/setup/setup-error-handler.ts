import { App } from 'vue';
import useNotifications from '@/components/notifications/use-notifications';
import { ApiAuthError, ApiRequestCancelledError } from '@/services/api/api-errors.ts';
import { $t } from '@/services/i18n';

export default function setupErrorHandler(app: App) {
  const { showErrorNotification } = useNotifications();
  app.config.errorHandler = (err, vm, info) => {
    if (err instanceof ApiRequestCancelledError) {
      // Do not show notification for cancelled requests
      return;
    }
    if (err instanceof ApiAuthError) {
      // Handled by the event system initiated from API client
      return;
    }
    console.error('Failure during component processing', err, vm, info);
    showErrorNotification($t.value.errorHandler.fatalErrorMessage());
  };
}
