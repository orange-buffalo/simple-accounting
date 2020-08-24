import Vue from 'vue';
import VueI18n from 'vue-i18n';
import Globalize from 'globalize';
import supportedLocaleCodes from '@/i18n/l10n/supported-locales.json';
import { loadCldrData, lookupClosestLocale } from '@/services/i18n/locale-utils';
import ICUFormatter from './i18n/icu-formatter';

Vue.use(VueI18n);

// register component with a name compliant with our code conventions
const i18nComponent = Vue.component('i18n');
Vue.component('I18n', i18nComponent);

const i18n = new VueI18n({});

let currentLocale;
let currentLanguage;
let currenciesInfo;
let defaultCurrencyDigits = 2;
let numbersInfo;
let numberParser;
let supportedLocales;
const supportedLanguages = [{
  languageCode: 'en',
  displayName: 'English',
}, {
  languageCode: 'uk',
  displayName: 'Українська',
}];

async function loadLanguage(language) {
  const { default: messages } = await import(/* webpackChunkName: "lang-[request]" */ `@/i18n/t9n/${language}`);
  i18n.setLocaleMessage(language, messages);
  i18n.locale = language;
  currentLanguage = language;
  supportedLocales = (await
  import(/* webpackChunkName: "[request]" */ `@/i18n/l10n/locales-display-names-${language}.json`)).default;
}

function buildCurrenciesInfo(cldr) {
  const cldrCurrencies = cldr.get('/main/{bundle}/numbers/currencies');
  currenciesInfo = [];
  Object.keys(cldrCurrencies)
    .forEach((code) => {
      const { displayName, symbol } = cldrCurrencies[code];
      currenciesInfo[code] = {
        code,
        displayName,
        symbol,
        digits: 2,
      };
    });

  const cldrCurrenciesFractions = cldr.get('/supplemental/currencyData/fractions');
  Object.keys(cldrCurrenciesFractions)
    .forEach((code) => {
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

function buildNumberInfo(cldr) {
  numbersInfo = cldr.get('/main/{bundle}/numbers/symbols-numberSystem-latn');
  numbersInfo = {
    decimalSymbol: numbersInfo.decimal,
    thousandsSeparator: numbersInfo.group,
  };
}

async function loadLocale(locale) {
  Globalize.load(await loadCldrData(locale));

  const globalize = Globalize(locale);
  const { cldr } = globalize;

  buildCurrenciesInfo(cldr);
  buildNumberInfo(cldr);
  numberParser = globalize.numberParser();

  i18n.formatter = new ICUFormatter({
    locale,
    globalize,
    i18n,
  });

  currentLocale = locale;
}

async function setupI18n(locale, language) {
  let loadLocaleDeferred;
  if (currentLocale !== locale) {
    loadLocaleDeferred = loadLocale(locale);
  }

  let loadLanguageDeferred;
  if (currentLanguage !== language) {
    loadLanguageDeferred = loadLanguage(language);
  }

  await Promise.all([loadLocaleDeferred, loadLanguageDeferred]);
}

function getValidLocale(requestedLocales) {
  return requestedLocales
    .map((requestedLocale) => lookupClosestLocale(requestedLocale, supportedLocaleCodes))
    .find((closestSupportedLocale) => closestSupportedLocale != null) || 'en';
}

function getValidLanguage(requestedLocales) {
  const supportedLanguageCodes = supportedLanguages.map((it) => it.languageCode);
  return requestedLocales
    .map((requestedLocale) => lookupClosestLocale(requestedLocale, supportedLanguageCodes))
    .find((closestSupportedLocale) => closestSupportedLocale != null) || 'en';
}

i18n.setLocaleFromBrowser = function setLocaleFromBrowser() {
  return setupI18n(
    getValidLocale(navigator.languages),
    getValidLanguage(navigator.languages),
  );
};

i18n.localeIdToLanguageTag = function localeIdToLanguageTag(localeId) {
  return localeId.replace(/_/g, '-');
};

i18n.languageTagToLocaleId = function languageTagToLocaleId(localeId) {
  return localeId.replace(/-/g, '_');
};

i18n.setLocaleFromProfile = function setLocaleFromProfile({ locale, language }) {
  return setupI18n(
    getValidLocale([this.localeIdToLanguageTag(locale)]),
    getValidLanguage([this.localeIdToLanguageTag(language)]),
  );
};

function emptyCurrencyInfo(code) {
  return {
    code,
    displayName: '',
    digits: defaultCurrencyDigits,
    symbol: '',
  };
}

i18n.getCurrenciesInfo = function getCurrenciesInfo() {
  return currenciesInfo;
};

i18n.getCurrencyInfo = function getCurrencyInfo(currency) {
  if (currency == null) {
    return emptyCurrencyInfo(currency);
  }
  const currencyInfo = currenciesInfo[currency];
  if (!currencyInfo) {
    console.warn(`${currency} is not supported`);
    return emptyCurrencyInfo(currency);
  }
  return currencyInfo;
};

i18n.getNumbersInfo = function getNumbersInfo() {
  return numbersInfo;
};

i18n.parserNumber = function parseNumber(input) {
  if (typeof input === 'number') {
    return input;
  }
  if (typeof input === 'string') {
    return numberParser(input);
  }
  return null;
};

i18n.getSupportedLocales = function getSupportedLocales() {
  return supportedLocales;
};

i18n.getSupportedLanguages = function getSupportedLanguages() {
  return supportedLanguages;
};

export default i18n;
