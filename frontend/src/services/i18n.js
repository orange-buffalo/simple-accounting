import Vue from 'vue';
import VueI18n from 'vue-i18n';

Vue.use(VueI18n);

const i18n = new VueI18n({});

const loadedLanguages = [];

function setLocale(locale) {
  i18n.locale = locale;
  document.querySelector('html')
    .setAttribute('lang', locale);
  return locale;
}

async function loadLanguage(locale) {
  if (i18n.locale === locale) {
    return setLocale(locale);
  }

  if (loadedLanguages.includes(locale)) {
    return Promise.resolve(setLocale(locale));
  }

  // If the language hasn't been loaded yet
  const { default: messages } = await import(/* webpackChunkName: "locale-[request]" */ `@/i18n/t9n/${locale}.js`);
  i18n.setLocaleMessage(locale, messages);
  loadedLanguages.push(locale);
  return setLocale(locale);
}

i18n.setLocaleFromBrowser = function setLocaleFromBrowser() {
  return loadLanguage('en');
};

i18n.setLocaleFromProfile = function setLocaleFromProfile({ locale, language }) {
  return loadLanguage(language);
};

export default i18n;
