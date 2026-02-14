import type { CldrStatic } from 'cldrjs';
import Globalize from 'globalize';
import baseCldrDataJson from '@/services/i18n/l10n/base.json?raw';

// do not transpile runtime data
const baseCldrData = JSON.parse(baseCldrDataJson);

export interface CurrencyInfo {
  code: string;
  displayName: string;
  symbol: string;
  digits: number;
}

let currenciesInfo: Record<string, CurrencyInfo>;
let defaultCurrencyDigits = 2;

function buildCurrenciesInfo(cldr: CldrStatic) {
  const cldrCurrencies = cldr.get('/main/{bundle}/numbers/currencies');
  currenciesInfo = {};
  Object.keys(cldrCurrencies).forEach((code) => {
    const { displayName, symbol } = cldrCurrencies[code];
    currenciesInfo[code] = {
      code,
      displayName,
      symbol,
      digits: 2,
    };
  });

  const cldrCurrenciesFractions = cldr.get('/supplemental/currencyData/fractions');
  Object.keys(cldrCurrenciesFractions).forEach((code) => {
    const { _digits: digits } = cldrCurrenciesFractions[code];
    if (code === 'DEFAULT') {
      defaultCurrencyDigits = digits;
    } else {
      const currencyInfo = currenciesInfo[code];
      if (currencyInfo == null) {
        console.warn(`${code} is not consistent`);
      } else if (digits != null) {
        currencyInfo.digits = digits;
      }
    }
  });
}

function emptyCurrencyInfo(code: string): CurrencyInfo {
  return {
    code,
    displayName: '',
    digits: defaultCurrencyDigits,
    symbol: '',
  };
}

export function getCurrencyInfo(currency: string) {
  if (currency == null) {
    return emptyCurrencyInfo(currency);
  }
  const currencyInfo = currenciesInfo[currency];
  if (!currencyInfo) {
    console.warn(`${currency} is not supported`);
    return emptyCurrencyInfo(currency);
  }
  return currencyInfo;
}

export function getCurrenciesInfo() {
  return currenciesInfo;
}

interface NumbersInfo {
  decimalSymbol: string;
  thousandsSeparator: string;
}

let numbersInfo: NumbersInfo;

function buildNumberInfo(cldr: CldrStatic) {
  const data = cldr.get('/main/{bundle}/numbers/symbols-numberSystem-latn');
  numbersInfo = {
    decimalSymbol: data.decimal,
    thousandsSeparator: data.group,
  };
}

export function getNumbersInfo(): NumbersInfo {
  return numbersInfo;
}

let globalize: Globalize;

async function loadCldrData(locale: string) {
  const localeDataJsonModule = await import(`./l10n/locale-${locale}.json?raw`);
  const localeDataJson: string = localeDataJsonModule.default;
  const localeCldrData = JSON.parse(localeDataJson);
  return [...baseCldrData, ...localeCldrData];
}

export async function updateLocale(locale: string) {
  Globalize.load(await loadCldrData(locale));

  globalize = Globalize(locale);
  const { cldr } = globalize;

  buildCurrenciesInfo(cldr);
  buildNumberInfo(cldr);
}

export function getCldr() {
  return globalize;
}
