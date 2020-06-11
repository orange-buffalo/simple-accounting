import MessageFormat from 'messageformat';
import amountFormatter from '@/services/i18n/amount-formatter';
import { dateTimeFormatter, dateFormatter } from '@/services/i18n/date-formatters';
import fileSizeFormatter from '@/services/i18n/file-size-formatter';
import yesNoFormatter from '@/services/i18n/yes-no-formatter';
import bpsFormatter from '@/services/i18n/bps-formatter';

function isVNode(node) {
  return node !== null && typeof node === 'object' && Object.prototype.hasOwnProperty.call(node, 'componentOptions');
}

// messageformat does not (obviously) support vue component, so component interpolation does not work
function shouldDelegateToDefaultFormatter(values) {
  if (values != null) {
    return Object.values(values)
      .find((it) => isVNode(it) || Object.values(it)
        .find((nested) => isVNode(nested)));
  }
  return false;
}

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
      fileSize: fileSizeFormatter({ globalize }),
      yesNo: yesNoFormatter({ i18n }),
      bps: bpsFormatter({ i18n }),
      // todo #206: rename when https://github.com/messageformat/messageformat/issues/274 is resolved
      saDateTime: dateTimeFormatter({ globalize }),
    });
  }

  interpolate(message, values) {
    if (shouldDelegateToDefaultFormatter(values)) {
      return null;
    }

    let formatter = this.cache[message];
    if (!formatter) {
      formatter = this.formatter.compile(message, this.locale);
      this.cache[message] = formatter;
    }
    return [formatter(values)];
  }
}
