import Vue from 'vue';
import VueI18n from 'vue-i18n';
import Cldr from 'cldrjs';
import Globalize from 'globalize';
import deepmerge from 'deepmerge';
import ICUFormatter from './i18n/icu-formatter';

Vue.use(VueI18n);

const i18n = new VueI18n({});

let currentLocale;
let currentLanguage;
let currenciesInfo;
let numbersInfo;

async function loadLanguage(language) {
  const { default: messages } = await import(/* webpackChunkName: "lang-[request]" */ `@/i18n/t9n/${language}`);
  i18n.setLocaleMessage(language, messages);
  i18n.locale = language;
  currentLanguage = language;
}

async function loadLocale(locale) {
  const { default: cldrData } = await
    import(/* webpackChunkName: "locale-[request]" */ `@/i18n/l10n/${locale}.cldr-data`);

  Cldr.load(cldrData);
  const cldr = new Cldr(locale);
  currenciesInfo = deepmerge(
    cldr.get('/main/{bundle}/numbers/currencies'),
    cldr.get('/supplemental/currencyData/fractions'),
  );
  numbersInfo = cldr.get('/main/{bundle}/numbers/symbols-numberSystem-latn');

  Globalize.load(cldrData);
  const globalize = Globalize(locale);

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

i18n.setLocaleFromBrowser = function setLocaleFromBrowser() {
  return setupI18n('en', 'en');
};

function localeIdToLanguageTag(localeId) {
  return localeId.replace(/_/g, '-');
}

i18n.setLocaleFromProfile = function setLocaleFromProfile({ locale, language }) {
  return setupI18n(localeIdToLanguageTag(locale), localeIdToLanguageTag(language));
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

export default i18n;
