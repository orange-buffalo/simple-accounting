export default function amountFormatter({ globalize, i18n }) {
  const currencyFormatters = [];

  return function amountFormatterImpl({ currency, amount }, locale, arg) {
    if (arg !== 'withCurrency') {
      throw Error(`${arg} is not supported for amount formatter`);
    }

    let currencyFormatter = currencyFormatters[currency];
    if (currencyFormatter == null) {
      currencyFormatter = globalize.currencyFormatter(currency);
      currencyFormatters[currency] = currencyFormatter;
    }

    return currencyFormatter(amount / (10 ** i18n.getCurrencyDigits(currency)));
  };
}
