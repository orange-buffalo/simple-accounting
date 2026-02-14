import { getCldr, getCurrencyInfo } from '@/services/i18n/cldr-data';

interface CurrencyAmount {
  currency: string;
  amountInCents: number;
}

export function amountFormatter() {
  const currencyFormatters: Record<string, (value: number) => string> = {};

  return function amountFormatterImpl(value: unknown, locale: string, arg: string | null) {
    if (arg !== 'withCurrency') {
      throw Error(`${arg} is not supported for amount formatter`);
    }
    const { currency, amountInCents } = value as CurrencyAmount;

    let currencyFormatter = currencyFormatters[currency];
    if (currencyFormatter == null) {
      currencyFormatter = getCldr().currencyFormatter(currency);
      currencyFormatters[currency] = currencyFormatter;
    }

    return currencyFormatter(amountInCents / 10 ** getCurrencyInfo(currency).digits);
  };
}
