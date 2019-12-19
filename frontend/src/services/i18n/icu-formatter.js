import MessageFormat from 'messageformat';
import amountFormatter from '@/services/i18n/amount-formatter';
import { dateTimeFormatter, dateFormatter } from '@/services/i18n/date-formatters';
import fileSizeFormatter from '@/services/i18n/file-size-formatter';

export default class ICUFormatter {
  constructor({
    locale,
    globalize,
    i18n,
  }) {
    this.locale = locale;
    this.formatter = new MessageFormat(this.locale);
    this.cache = {};
    this.formatter.addFormatters({
      amount: amountFormatter({
        globalize,
        i18n,
      }),
      date: dateFormatter({ globalize }),
      fileSize: fileSizeFormatter(),
      // todo #6: rename when https://github.com/messageformat/messageformat/issues/274 is resolved
      saDateTime: dateTimeFormatter({ globalize }),
    });
  }

  interpolate(message, values) {
    let formatter = this.cache[message];
    if (!formatter) {
      formatter = this.formatter.compile(message, this.locale);
      this.cache[message] = formatter;
    }
    return [formatter(values)];
  }
}
