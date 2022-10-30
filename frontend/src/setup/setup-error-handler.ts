import useNotifications from '@/components/notifications/use-notifications';
import { API_FATAL_ERROR_EVENT } from '@/services/events';
import { $t } from '@/services/i18n';

export default function setupErrorHandler() {
  const { showErrorNotification } = useNotifications();
  API_FATAL_ERROR_EVENT.subscribe(() => {
    showErrorNotification($t.value.errorHandler.fatalApiError());
  });
}
