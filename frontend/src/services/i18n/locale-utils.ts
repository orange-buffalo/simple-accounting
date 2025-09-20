// https://github.com/format-message/format-message/blob/master/packages/lookup-closest-locale/index.js
export function lookupClosestLocale(requestedLocale: string, availableLocales: string[]) {
  if (availableLocales.includes(requestedLocale)) {
    return requestedLocale;
  }
  const locales = ([] as string[]).concat([requestedLocale]);

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
