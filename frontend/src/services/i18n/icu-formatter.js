import MessageFormat from 'messageformat';
import amountFormatter from '@/services/i18n/amount-formatter';

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
