import useNotifications from '@/components/useNotifications';
import { API_FATAL_ERROR_EVENT } from '@/services/events';
import i18n from '@/services/i18n';

export default function setupErrorHandler() {
  const { showErrorNotification } = useNotifications();
  API_FATAL_ERROR_EVENT.subscribe(() => {
    showErrorNotification({
      message: $t.errorHandler.fatalApiError(),
    });
  });
}
