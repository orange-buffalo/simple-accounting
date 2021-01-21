import baseCldrData from '@/../generatedSrc/i18n/l10n/base.json';

// https://github.com/format-message/format-message/blob/master/packages/lookup-closest-locale/index.js
export function lookupClosestLocale(requestedLocale, availableLocales) {
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

export async function loadCldrData(locale) {
  const { default: localeCldrData } = await
  import(/* webpackChunkName: "[request]" */ `@/../generatedSrc/i18n/l10n/locale-${locale}.json`);
  return [...baseCldrData, ...localeCldrData];
}
