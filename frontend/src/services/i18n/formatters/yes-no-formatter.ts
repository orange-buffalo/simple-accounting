import { i18n } from '@/services/i18n';

export function yesNoFormatter() {
  return function yesNoFormatterImpl(value: unknown) {
    const booleanValue = value as boolean;
    if (booleanValue) {
      return i18n.t('common.yesNo.yes');
    }
    return i18n.t('common.yesNo.no');
  };
}
