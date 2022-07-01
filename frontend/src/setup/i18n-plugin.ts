import type { App } from 'vue';
import { i18n } from '@/services/i18n';

export function i18nPlugin(vueApp: App) {
  // eslint-disable-next-line no-param-reassign,operator-linebreak
  vueApp.config.globalProperties.$t =
    (messageKey: string, values?: Record<string, unknown> | unknown[]) => i18n.t(messageKey, values);
}
