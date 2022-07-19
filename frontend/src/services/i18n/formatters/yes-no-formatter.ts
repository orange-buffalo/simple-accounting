import { $t } from '@/services/i18n';

export function yesNoFormatter() {
  return function yesNoFormatterImpl(value: unknown) {
    const booleanValue = value as boolean;
    if (booleanValue) {
      return $t.value.common.yesNo.yes();
    }
    return $t.value.common.yesNo.no();
  };
}
