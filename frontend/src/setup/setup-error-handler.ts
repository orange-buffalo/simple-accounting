import useNotifications from '@/components/notifications/use-notifications';
import { API_BAD_REQUEST_EVENT, API_FATAL_ERROR_EVENT } from '@/services/events';
import { $t } from '@/services/i18n';

export default function setupErrorHandler() {
  const { showErrorNotification, showWarningNotification } = useNotifications();
  API_FATAL_ERROR_EVENT.subscribe(() => {
    showErrorNotification($t.value.errorHandler.fatalApiError());
  });
  API_BAD_REQUEST_EVENT.subscribe(() => {
    showWarningNotification($t.value.errorHandler.badRequestError());
  });
}
