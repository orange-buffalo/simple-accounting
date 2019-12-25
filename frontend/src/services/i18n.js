import Vue from 'vue';
import VueI18n from 'vue-i18n';
import Globalize from 'globalize';
import deepmerge from 'deepmerge';
import ICUFormatter from './i18n/icu-formatter';
import baseCldrData from '@/i18n/l10n/base.json';
import supportedLocaleCodes from '@/i18n/l10n/locales.json';

Vue.use(VueI18n);

const i18n = new VueI18n({});

let currentLocale;
let currentLanguage;
let currenciesInfo;
let numbersInfo;
let numberParser;
let supportedLocales;
const supportedLanguages = [{
  languageCode: 'en',
  displayName: 'English',
}, {
  languageCode: 'uk',
  displayName: 'Ukrainian',
}];

async function loadLanguage(language) {
  const { default: messages } = await import(/* webpackChunkName: "lang-[request]" */ `@/i18n/t9n/${language}`);
  i18n.setLocaleMessage(language, messages);
  i18n.locale = language;
  currentLanguage = language;
  // todo #6: load locale names in this language, check myprofile is updated
}

function loadSupportedLocales(cldr) {
  // todo #6: many locale have no display name, check how to fix that
  supportedLocales = supportedLocaleCodes.map(localeCode => ({
    locale: localeCode,
    displayName: cldr.main(`localeDisplayNames/languages/${localeCode}`),
  }));
}

async function loadLocale(locale) {
  const { default: localeCldrData } = await import(
    /* webpackChunkName: "locale-[request]" */
    /* webpackExclude: /locales.json|base.json$/ */
    // eslint-disable-next-line comma-dangle
    `@/i18n/l10n/${locale}.json`
    // eslint-disable-next-line
    );
  const cldrData = [...baseCldrData, ...localeCldrData];
  Globalize.load(cldrData);

  const globalize = Globalize(locale);
  const { cldr } = globalize;

  loadSupportedLocales(cldr);

  currenciesInfo = deepmerge(
    cldr.get('/main/{bundle}/numbers/currencies'),
    cldr.get('/supplemental/currencyData/fractions'),
  );

  numbersInfo = cldr.get('/main/{bundle}/numbers/symbols-numberSystem-latn');

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

// https://github.com/format-message/format-message/blob/master/packages/lookup-closest-locale/index.js
function lookupClosestLocale(requestedLocale, availableLocales) {
  if (availableLocales.includes(requestedLocale)) {
    return requestedLocale;
  }
  const locales = [].concat(requestedLocale || []);
  // eslint-disable-next-line
  for (let l = 0, ll = locales.length; l < ll; ++l) {
    const current = locales[l].split('-');
    while (current.length) {
      const candidate = current.join('-');
      if (availableLocales.includes(candidate)) {
        return candidate;
      }
      current.pop();
    }
  }
  return null;
}

function getValidLocale(requestedLocales) {
  return requestedLocales
    .map(requestedLocale => lookupClosestLocale(requestedLocale, supportedLocaleCodes))
    .find(closestSupportedLocale => closestSupportedLocale != null) || 'en';
}

function getValidLanguage(requestedLocales) {
  const supportedLanguageCodes = supportedLanguages.map(it => it.languageCode);
  return requestedLocales
    .map(requestedLocale => lookupClosestLocale(requestedLocale, supportedLanguageCodes))
    .find(closestSupportedLocale => closestSupportedLocale != null) || 'en';
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

i18n.getCurrencyInfo = function getCurrencyInfo(currency) {
  const currencyInfo = currenciesInfo[currency];
  if (!currencyInfo) {
    console.warn(`${currency} is not supported`);
    return {};
  }
  return currencyInfo;
};

i18n.getCurrencyDigits = function getCurrencyDigits(currency) {
  const currencyInfo = this.getCurrencyInfo(currency);
  // eslint-disable-next-line no-underscore-dangle
  return currencyInfo._digits ? currencyInfo._digits : 2;
};

i18n.getCurrencySymbol = function getCurrencySymbol(currency) {
  const currencyInfo = this.getCurrencyInfo(currency);
  return currencyInfo.symbol ? currencyInfo.symbol : '';
};

i18n.getCurrencyDisplayName = function getCurrencyDisplayName(currency) {
  const currencyInfo = this.getCurrencyInfo(currency);
  return currencyInfo.displayName ? currencyInfo.displayName : '';
};

i18n.getNumbersInfo = function getNumbersInfo() {
  return numbersInfo;
};

i18n.getDecimalSeparator = function getDecimalSeparator() {
  return this.getNumbersInfo().decimal;
};

i18n.getThousandSeparator = function getThousandSeparator() {
  return this.getNumbersInfo().group;
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
