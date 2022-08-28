import { formatMessage } from '@/services/i18n';

export function bpsFormatter() {
  return function bpsFormatterImpl(value: unknown, locale: string, arg: string | null) {
    const bpsValue = value as number;
    if (arg !== 'percent') {
      throw Error(`${arg} is not supported for bps formatter`);
    }
    if (bpsValue == null) {
      return null;
    }
    return formatMessage('{0, number, :: percent scale/100 .##}', [bpsValue / 10000]);
  };
}
